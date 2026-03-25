package com.bx.implatform.listener;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bx.imclient.annotation.IMListener;
import com.bx.imclient.listener.MessageListener;
import com.bx.imcommon.enums.IMListenerType;
import com.bx.imcommon.enums.IMSendCode;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMSendResult;
import com.bx.implatform.entity.PrivateMessage;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.service.OfflineNotifyService;
import com.bx.implatform.service.PrivateMessageCompanyService;
import com.bx.implatform.vo.PrivateMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Slf4j
@IMListener(type = IMListenerType.PRIVATE_MESSAGE)
public class PrivateMessageListener implements MessageListener<PrivateMessageVO> {

    @Lazy
    @Autowired
    private PrivateMessageCompanyService privateMessageService;

    @Lazy
    @Autowired
    private OfflineNotifyService offlineNotifyService;

    @Override
    public void process(List<IMSendResult<PrivateMessageVO>> results) {
        // 更新消息状态
        updateMessageStatus(results);
        // 推送离线通知
        sendOfflineNotify(results);
    }

    private void updateMessageStatus(List<IMSendResult<PrivateMessageVO>> results) {
        Set<Long> messageIds = new HashSet<>();
        for (IMSendResult<PrivateMessageVO> result : results) {
            PrivateMessageVO messageInfo = result.getData();
            MessageType messageType = MessageType.fromCode(messageInfo.getType());
            // 更新消息状态,这里只处理成功消息，失败的消息继续保持未读状态
            if (result.getCode().equals(IMSendCode.SUCCESS.code())) {
                // 只有普通消息和操作交互类消息有入库
                if (messageType.isNormal() || messageType.isAct() || messageType.isTip() || messageType.equals(MessageType.RECALL)) {
                    if(result.getReceiver().getId().equals(messageInfo.getRecvId())){
                        messageIds.add(messageInfo.getId());
                        log.debug("消息送达，消息id:{}，发送者:{},接收者:{},终端:{}",
                                messageInfo.getId(), result.getSender().getId(), result.getReceiver().getId(), result.getReceiver().getTerminal());
                    }
                }
            }
        }
        // 对发送成功的消息修改状态
        if (CollUtil.isNotEmpty(messageIds)) {
            UpdateWrapper<PrivateMessage> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda()
                    .in(PrivateMessage::getId, messageIds)
                    .eq(PrivateMessage::getStatus, MessageStatus.PENDING.code())
                    .set(PrivateMessage::getStatus, MessageStatus.DELIVERED.code());
            privateMessageService.update(updateWrapper);
        }
    }

    private void sendOfflineNotify(List<IMSendResult<PrivateMessageVO>> results) {
        // 针对APP离线用户进行离线通知
        List<IMSendResult<PrivateMessageVO>> notifyResults = new LinkedList<>();
        for (IMSendResult<PrivateMessageVO> result : results) {
            MessageType messageType = MessageType.fromCode(result.getData().getType());
            if (result.getCode().equals(IMSendCode.NOT_ONLINE.code()) && result.getReceiver().getTerminal()
                .equals(IMTerminalType.APP.code())) {
                if (messageType.isNormal()) {
                    // 普通消息批量处理
                    notifyResults.add(result);
                } else if (messageType.isRequest()) {
                    // 好友验证请求
                    offlineNotifyService.sendFriendRequestNotify(result.getData());
                }
            }
        }
        // 推送
        if (!notifyResults.isEmpty()) {
            offlineNotifyService.sendPrivateOfflineNotify(notifyResults);
        }
    }
}
