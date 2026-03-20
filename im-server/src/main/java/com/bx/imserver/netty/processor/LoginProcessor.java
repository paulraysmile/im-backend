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
        String strInfo = JwtUtil.getInfo(loginInfo.getAccessToken());
        IMSessionInfo sessionInfo = JSON.parseObject(strInfo, IMSessionInfo.class);
        Long userId = sessionInfo.getUserId();
        Integer terminal = sessionInfo.getTerminal();
        String devId = StrUtil.isEmpty(loginInfo.getDevId()) ? "" : loginInfo.getDevId();
        log.info("用户登录，userId:{},终端:{},设备:{}", userId, terminal, devId);

        // APP 端仅允许单设备：同 terminal 已有连接则踢下线
        if (IMTerminalType.APP.code().equals(terminal)) {
            ChannelHandlerContext oldCtx = UserChannelCtxMap.getChannelCtx(userId, terminal);
            if (oldCtx != null && !ctx.channel().id().equals(oldCtx.channel().id())) {
                AttributeKey<String> devIdAttrKey = AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID);
                String oldDevId = oldCtx.channel().attr(devIdAttrKey).get();
                IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                sendInfo.setCmd(IMCmdType.FORCE_LOGUT.code());
                sendInfo.setData("您已在其他设备登录，将被强制下线");
                oldCtx.channel().writeAndFlush(sendInfo);
                UserChannelCtxMap.removeChannelCtx(userId, terminal, oldDevId);
                String appKey = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
                redisMQTemplate.delete(appKey);
                log.info("APP 单设备策略，踢旧连接,userId:{}", userId);
            }
        }
        // WEB 端允许多设备，不踢旧连接

        // 绑定用户和 channel（带 deviceId）
        UserChannelCtxMap.addChannelCtx(userId, terminal, devId, ctx);
        // 设置用户 id 属性
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        ctx.channel().attr(userIdAttr).set(userId);
        // 设置用户终端类型
        AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
        ctx.channel().attr(terminalAttr).set(terminal);
        // 设置用户设备 id
        AttributeKey<String> devIdAttr = AttributeKey.valueOf(ChannelAttrKey.DEVICE_ID);
        ctx.channel().attr(devIdAttr).set(devId);
        // 初始化心跳次数
        AttributeKey<Long> heartBeatAttr = AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES);
        ctx.channel().attr(heartBeatAttr).set(0L);

        // Redis 在线标记：APP 单 key；WEB 按 deviceId 多 key
        if (IMTerminalType.APP.code().equals(terminal)) {
            String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
            redisMQTemplate.opsForValue()
                .set(key, IMServerGroup.serverId, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
        } else {
            String webKey = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.toString(),
                devId.isEmpty() ? "default" : devId);
            redisMQTemplate.opsForValue()
                .set(webKey, IMServerGroup.serverId, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
        }
        // 推送用户上线事件给业务层
        IMUserEvent event = new IMUserEvent();
        event.setEventType(IMEventType.ONLINE.code());
        event.setUserInfo(new IMUserInfo(userId, terminal));
        redisMQTemplate.opsForList().rightPush(ChatRedisKey.IM_USER_EVENT_QUEUE, event);
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
