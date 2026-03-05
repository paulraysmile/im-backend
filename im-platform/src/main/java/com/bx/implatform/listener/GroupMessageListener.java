package com.bx.implatform.listener;

import com.bx.imclient.annotation.IMListener;
import com.bx.imclient.listener.MessageListener;
import com.bx.imcommon.enums.IMListenerType;
import com.bx.imcommon.enums.IMSendCode;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMSendResult;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.service.OfflineNotifyService;
import com.bx.implatform.vo.GroupMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@IMListener(type = IMListenerType.GROUP_MESSAGE)
public class GroupMessageListener implements MessageListener<GroupMessageVO> {

    @Lazy
    @Autowired
    private OfflineNotifyService offlineNotifyService;

    @Override
    public void process(List<IMSendResult<GroupMessageVO>> results) {
        for(IMSendResult<GroupMessageVO> result:results){
            GroupMessageVO messageInfo = result.getData();
            if (result.getCode().equals(IMSendCode.SUCCESS.code())) {
              //  log.info("消息送达，消息id:{}，发送者:{},接收者:{},终端:{}", messageInfo.getId(), result.getSender().getId(), result.getReceiver().getId(), result.getReceiver().getTerminal());
            }
        }
        // 推送离线通知
        sendOfflineNotify(results);
    }

    private void sendOfflineNotify(List<IMSendResult<GroupMessageVO>> results) {
        // 针对APP离线用户进行离线通知
        List<IMSendResult<GroupMessageVO>> notifyResults = new LinkedList<>();
        for (IMSendResult<GroupMessageVO> result : results) {
            MessageType messageType = MessageType.fromCode(result.getData().getType());
            if (result.getCode().equals(IMSendCode.NOT_ONLINE.code()) && result.getReceiver().getTerminal()
                .equals(IMTerminalType.APP.code()) && messageType.isNormal()) {
                notifyResults.add(result);
            }
        }
        // 推送
        if(!notifyResults.isEmpty()){
            offlineNotifyService.sendGroupOfflineNotify(notifyResults);
        }
    }
}
