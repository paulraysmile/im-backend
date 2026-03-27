package com.bx.imserver.netty.processor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.bx.imcommon.contant.ChatConstant;
import com.bx.imcommon.contant.ChatRedisKey;
import com.bx.imcommon.enums.IMCmdType;
import com.bx.imcommon.enums.IMEventType;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.*;
import com.bx.imcommon.mq.RedisMQTemplate;
import com.bx.imcommon.util.JwtUtil;
import com.bx.imserver.constant.ChannelAttrKey;
import com.bx.imserver.netty.IMServerGroup;
import com.bx.imserver.netty.UserChannelCtxMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginProcessor extends AbstractMessageProcessor<IMLoginInfo> {

    private final RedisMQTemplate redisMQTemplate;

    @Value("${jwt.accessToken.secret}")
    private String accessTokenSecret;

    @Override
    public void process(ChannelHandlerContext ctx, IMLoginInfo loginInfo) {
        if (!JwtUtil.checkSign(loginInfo.getAccessToken(), accessTokenSecret)) {
            ctx.channel().close();
            log.warn("用户token校验不通过，强制下线,token:{}", loginInfo.getAccessToken());
            return;
        }
        IMSessionInfo sessionInfo = JSON.parseObject(JwtUtil.getInfo(loginInfo.getAccessToken()), IMSessionInfo.class);
        Long userId = sessionInfo.getUserId();
        Integer terminal = sessionInfo.getTerminal();
        log.info("用户登录，userId:{}，terminal:{}", userId, terminal);
        String devId = StrUtil.isBlank(loginInfo.getDevId()) ? "default" : loginInfo.getDevId();
        // APP端仅允许单设备
        if (IMTerminalType.APP.code().equals(terminal)) {
            ChannelHandlerContext context = UserChannelCtxMap.getChannelCtx(userId, terminal);
            if (context != null && !ctx.channel().id().equals(context.channel().id())) {
                if ("default".equals(devId) || !loginInfo.getDevId().equals(context.channel().attr(AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID)).get())) {
                    // 不允许多地登录,强制下线
                    IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                    sendInfo.setCmd(IMCmdType.FORCE_LOGUT.code());
                    sendInfo.setData("您已在其他地方登录，将被强制下线");
                    context.channel().writeAndFlush(sendInfo);
                    log.info("异地登录，强制下线,userId:{},终端:{}", userId, terminal);
                }
            }
            // 绑定用户和channel
            UserChannelCtxMap.addChannelCtx(userId, terminal, ctx);
            // 设置用户id属性
            AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
            ctx.channel().attr(userIdAttr).set(userId);
            // 设置用户终端类型
            AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
            ctx.channel().attr(terminalAttr).set(terminal);
            // 设置用户设备id
            AttributeKey<String> devIdAttr = AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID);
            ctx.channel().attr(devIdAttr).set(devId);
            // 初始化心跳次数
            AttributeKey<Long> heartBeatAttr = AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES);
            ctx.channel().attr(heartBeatAttr).set(0L);
            // Redis 在线标记：APP 单 key；WEB 按 deviceId 多 key
            // 在redis上记录每个user的channelId，15秒没有心跳，则自动过期
            if (IMTerminalType.APP.code().equals(terminal)) {
                String userServerKey = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
                redisMQTemplate.opsForValue().set(userServerKey, IMServerGroup.serverId, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
                redisMQTemplate.opsForValue().set(userServerKey, IMServerGroup.serverId, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);

            } else {
                String webKey = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString(), devId);
                redisMQTemplate.opsForValue().set(webKey, IMServerGroup.serverId, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
                String userDeviceKey = String.join(":", ChatRedisKey.IM_USER_DEVICE_ID, userId.toString(), terminal.toString());
                redisMQTemplate.opsForSet().add(userDeviceKey, devId);
            }



        } else {



        }

        // 推送用户上线事件给业务层
        IMUserEvent event = new IMUserEvent();
        event.setEventType(IMEventType.ONLINE.code());
        event.setUserInfo(new IMUserInfo(userId, terminal));
        key = ChatRedisKey.IM_USER_EVENT_QUEUE;
        redisMQTemplate.opsForList().rightPush(key, event);
        // 响应ws
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.LOGIN.code());
        ctx.channel().writeAndFlush(sendInfo);
    }

    @Override
    public IMLoginInfo transForm(Object o) {
        HashMap map = (HashMap)o;
        return BeanUtil.fillBeanWithMap(map, new IMLoginInfo(), false);
    }
}
