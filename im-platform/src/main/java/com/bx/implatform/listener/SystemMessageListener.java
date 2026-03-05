package com.bx.implatform.listener;

import com.bx.imclient.annotation.IMListener;
import com.bx.imclient.listener.MessageListener;
import com.bx.imcommon.enums.IMListenerType;
import com.bx.imcommon.enums.IMSendCode;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMSendResult;
import com.bx.implatform.service.OfflineNotifyService;
import com.bx.implatform.vo.SystemMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@IMListener(type = IMListenerType.SYSTEM_MESSAGE)
public class SystemMessageListener implements MessageListener<SystemMessageVO> {

    @Lazy
    @Autowired
    private OfflineNotifyService offlineNotifyService;

    @Override
    public void process(List<IMSendResult<SystemMessageVO>> results) {
        // 推送离线通知
        sendOfflineNotify(results);
    }

    private void sendOfflineNotify(List<IMSendResult<SystemMessageVO>> results) {
        // 针对APP离线用户进行离线通知
        List<IMSendResult<SystemMessageVO>> notifyResults = new LinkedList<>();
        for (IMSendResult<SystemMessageVO> result : results) {
            if (result.getCode().equals(IMSendCode.NOT_ONLINE.code()) && result.getReceiver().getTerminal()
                .equals(IMTerminalType.APP.code())) {
                notifyResults.add(result);
            }
        }
        // 推送
        if(!notifyResults.isEmpty()){
            offlineNotifyService.sendSystemOfflineNotify(notifyResults);
        }
    }
}
