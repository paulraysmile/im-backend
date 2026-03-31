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
import io.netty.channel.Channel;
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
        String devId = loginInfo.getDevId();
        String accessToken = loginInfo.getAccessToken();
        Channel channel = ctx.channel();
        if (!JwtUtil.checkSign(accessToken, accessTokenSecret)) {
            channel.close();
            log.warn("用户token校验不通过，强制下线,token:{}", accessToken);
            return;
        }
        IMSessionInfo sessionInfo = JSON.parseObject(JwtUtil.getInfo(accessToken), IMSessionInfo.class);
        Long userId = sessionInfo.getUserId();
        Integer terminal = sessionInfo.getTerminal();
        log.info("用户登录，userId:{}，terminal:{}", userId, terminal);
        AttributeKey<String> devIdAttr = AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID);
        // APP端仅允许单设备
        if (IMTerminalType.APP.code().equals(terminal)) {
            ChannelHandlerContext context = UserChannelCtxMap.getAppChannelCtx(userId);
            if (context != null && !ctx.channel().id().equals(context.channel().id())) {
                if (StrUtil.isEmpty(devId) || !devId.equals(context.channel().attr(devIdAttr).get())) {
                    // 不允许多地登录,强制下线
                    IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                    sendInfo.setCmd(IMCmdType.FORCE_LOGUT.code());
                    sendInfo.setData("您已在其他地方登录，将被强制下线");
                    context.channel().writeAndFlush(sendInfo);
                    log.info("异地登录，强制下线,userId:{},终端:{}", userId, terminal);
                }
            }
            // 绑定用户和channel
            UserChannelCtxMap.addAppChannelCtx(userId, ctx);
            // 在redis上记录每个user的channelId，15秒没有心跳，则自动过期
            String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
            redisMQTemplate.opsForValue().set(key, IMServerGroup.serverId, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
        } else {
            // 绑定用户和channel
            UserChannelCtxMap.addWebChannelCtx(userId, terminal, ctx);
            String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
            redisMQTemplate.opsForValue().set(key, IMServerGroup.serverId, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);



            // 在redis上记录每个user的channelId，15秒没有心跳，则自动过期
            String key = String.join(":", ChatRedisKey.IM_USER_WEB_SERVER_ID, userId.toString(), terminal.toString());
            redisMQTemplate.opsForSet().add(key, IMServerGroup.serverId);
            redisMQTemplate.expire(key, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
            key = String.join(":", ChatRedisKey.IM_USER_WEB_SERVER_COUNT, userId.toString(), terminal.toString(), String.valueOf(IMServerGroup.serverId));
            Long incremented = redisMQTemplate.opsForValue().increment(key);
            if (incremented == 1) {
                redisMQTemplate.expire(key, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
            }
        }
        // 设置用户id属性
        channel.attr(AttributeKey.valueOf(ChannelAttrKey.USER_ID)).set(userId);
        // 设置用户终端类型
        channel.attr(AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE)).set(terminal);
        // 设置用户设备id
        channel.attr(devIdAttr).set(devId);
        // 初始化心跳次数
        channel.attr(AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES)).set(0L);
        // 推送用户上线事件给业务层
        IMUserEvent event = new IMUserEvent();
        event.setEventType(IMEventType.ONLINE.code());
        event.setUserInfo(new IMUserInfo(userId, terminal));
        redisMQTemplate.opsForList().rightPush(ChatRedisKey.IM_USER_EVENT_QUEUE, event);
        // 响应ws
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.LOGIN.code());
        channel.writeAndFlush(sendInfo);
    }

    @Override
    public IMLoginInfo transForm(Object o) {
        HashMap map = (HashMap)o;
        return BeanUtil.fillBeanWithMap(map, new IMLoginInfo(), false);
    }
}
