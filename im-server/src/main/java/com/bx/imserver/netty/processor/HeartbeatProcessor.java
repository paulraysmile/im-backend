package com.bx.imserver.netty.processor;

import cn.hutool.core.bean.BeanUtil;
import com.bx.imcommon.contant.ChatConstant;
import com.bx.imcommon.contant.ChatRedisKey;
import com.bx.imcommon.enums.IMCmdType;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMHeartbeatInfo;
import com.bx.imcommon.model.IMSendInfo;
import com.bx.imcommon.mq.RedisMQTemplate;
import com.bx.imserver.constant.ChannelAttrKey;
import com.bx.imserver.netty.IMServerGroup;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatProcessor extends AbstractMessageProcessor<IMHeartbeatInfo> {

    private final RedisMQTemplate redisMQTemplate;

    @Override
    public void process(ChannelHandlerContext ctx, IMHeartbeatInfo beatInfo) {
        Channel channel = ctx.channel();
        // 响应ws
        IMSendInfo<Object> sendInfo = new IMSendInfo<>();
        sendInfo.setCmd(IMCmdType.HEART_BEAT.code());
        channel.writeAndFlush(sendInfo);
        // 设置属性
        AttributeKey<Long> heartBeatAttr = AttributeKey.valueOf(ChannelAttrKey.HEARTBEAT_TIMES);
        Long heartbeatTimes = channel.attr(heartBeatAttr).get();
        channel.attr(heartBeatAttr).set(++heartbeatTimes);
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        Long userId = channel.attr(userIdAttr).get();
        if (heartbeatTimes % 10 == 0) {
            // 每心跳10次，用户在线状态续一次命
            AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
            Integer terminal = ctx.channel().attr(terminalAttr).get();
            if (IMTerminalType.APP.code().equals(terminal)) {
                String key = String.join(":", ChatRedisKey.IM_USER_APP_SERVER_ID, userId.toString(), terminal.toString());
                redisMQTemplate.expire(key, ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
            } else {
                redisMQTemplate.expire(String.join(":", ChatRedisKey.IM_USER_WEB_SERVER_ID, userId.toString(), terminal.toString()),
                        ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
                redisMQTemplate.expire(String.join(":", ChatRedisKey.IM_USER_WEB_SERVER_COUNT, userId.toString(), terminal.toString(), String.valueOf(IMServerGroup.serverId)),
                        ChatConstant.ONLINE_TIMEOUT_SECOND, TimeUnit.SECONDS);
            }
        }
        log.debug("心跳,userId:{},{}", userId, channel.id().asLongText());
    }

    @Override
    public IMHeartbeatInfo transForm(Object o) {
        HashMap map = (HashMap) o;
        return BeanUtil.fillBeanWithMap(map, new IMHeartbeatInfo(), false);
    }
}
