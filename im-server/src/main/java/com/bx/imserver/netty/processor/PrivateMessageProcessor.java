package com.bx.imserver.netty.processor;

import cn.hutool.core.util.StrUtil;
import com.bx.imcommon.contant.ChatRedisKey;
import com.bx.imcommon.enums.IMCmdType;
import com.bx.imcommon.enums.IMSendCode;
import com.bx.imcommon.model.IMRecvInfo;
import com.bx.imcommon.model.IMSendInfo;
import com.bx.imcommon.model.IMSendResult;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.imcommon.mq.RedisMQTemplate;
import com.bx.imserver.netty.UserChannelCtxMap;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageProcessor extends AbstractMessageProcessor<IMRecvInfo> {

    private final RedisMQTemplate redisMQTemplate;

    @Override
    public void process(IMRecvInfo recvInfo) {
        IMUserInfo sender = recvInfo.getSender();
        List<IMUserInfo> receivers = recvInfo.getReceivers();
        if (receivers == null || receivers.isEmpty()) {
            return;
        }
        log.info("接收到私聊消息，发送者:{},接收者数:{}，内容:{}", sender.getId(), receivers.size(), recvInfo.getData());
        try {
            int pushed = 0;
            for (IMUserInfo receiver : receivers) {
                ChannelHandlerContext channelCtx = UserChannelCtxMap.getChannelCtx(
                    receiver.getId(), receiver.getTerminal(), receiver.getDeviceId());
                if (channelCtx != null) {
                    IMSendInfo<Object> sendInfo = new IMSendInfo<>();
                    sendInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
                    sendInfo.setData(recvInfo.getData());
                    channelCtx.channel().writeAndFlush(sendInfo);
                    pushed++;
                } else {
                    log.warn("未找到channel，接收者:{},终端:{},设备:{}", receiver.getId(), receiver.getTerminal(),
                        receiver.getDeviceId());
                }
            }
            if (recvInfo.getSendResult()) {
                sendResult(recvInfo, pushed > 0 ? IMSendCode.SUCCESS : IMSendCode.NOT_FIND_CHANNEL, receivers.get(0));
            }
        } catch (Exception e) {
            if (recvInfo.getSendResult()) {
                sendResult(recvInfo, IMSendCode.UNKONW_ERROR, receivers.get(0));
            }
            log.error("发送异常，发送者:{},接收者:{}，内容:{}", sender.getId(), receivers, recvInfo.getData(), e);
        }
    }

    private void sendResult(IMRecvInfo recvInfo, IMSendCode sendCode, IMUserInfo receiver) {
        if (!Boolean.TRUE.equals(recvInfo.getSendResult())) {
            return;
        }
        IMSendResult<Object> result = new IMSendResult<>();
        result.setSender(recvInfo.getSender());
        result.setReceiver(receiver);
        result.setCode(sendCode.code());
        result.setData(recvInfo.getData());
        String key = StrUtil.join(":", ChatRedisKey.IM_RESULT_PRIVATE_QUEUE, recvInfo.getServiceName());
        redisMQTemplate.opsForList().rightPush(key, result);
    }
}
