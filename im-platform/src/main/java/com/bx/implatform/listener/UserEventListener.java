package com.bx.implatform.listener;

import com.bx.imclient.IMClient;
import com.bx.imclient.annotation.IMListener;
import com.bx.imclient.listener.EventListener;
import com.bx.imcommon.enums.IMEventType;
import com.bx.imcommon.enums.IMListenerType;
import com.bx.imcommon.model.IMUserEvent;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.implatform.enums.UserStateType;
import com.bx.implatform.service.FriendService;
import com.bx.implatform.service.WebrtcGroupService;
import com.bx.implatform.service.WebrtcPrivateService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.UserStateUtils;
import com.bx.implatform.vo.UserStateVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户事件监听
 *
 * @author Blue
 * @version 1.0
 */
@Slf4j
@IMListener(type = IMListenerType.USER_EVENT)
@AllArgsConstructor
public class UserEventListener implements EventListener {

    private final IMClient imClient;

    private final FriendService friendService;

    private final UserStateUtils userStateUtils;
    private final WebrtcPrivateService webrtcPrivateService;
    private final WebrtcGroupService webrtcGroupService;

    @Override
    public void process(List<IMUserEvent> events) {
        for (IMUserEvent event : events) {
            if (event.getEventType().equals(IMEventType.ONLINE.code())) {
                // 向好友推送在线状态
                IMUserInfo userInfo = event.getUserInfo();
                friendService.sendOnlineStatus(userInfo.getId(), userInfo.getTerminal());
                log.info("用户上线,id:{},终端：{}", userInfo.getId(), userInfo.getTerminal());
            } else if (event.getEventType().equals(IMEventType.OFFLINE.code())) {
                // 向好友推送在线状态
                IMUserInfo userInfo = event.getUserInfo();
                friendService.sendOnlineStatus(userInfo.getId(), userInfo.getTerminal());
                // 如果用户在通话时强制退出，会造成对方画面卡死
                if (userStateUtils.isBusy(userInfo.getId())) {
                    // 模拟该用户进行通话挂断
                    simulateQuitRtc(userInfo);
                }
                log.info("用户下线,id:{},终端：{}", userInfo.getId(), userInfo.getTerminal());
            }
        }
    }

    private void simulateQuitRtc(IMUserInfo userInfo) {
        try {
            // 模拟用户登录
            UserSession session = new UserSession();
            session.setUserId(userInfo.getId());
            session.setTerminal(userInfo.getTerminal());
            SessionContext.setSession(session);
            // 模拟挂断通话
            UserStateVO state = userStateUtils.getState(userInfo.getId());
            if (state.getType().equals(UserStateType.RTC_PRIVATE.getCode())) {
                // 强制退出通话
                webrtcPrivateService.forceQuit((Long)state.getData());
            } else if (state.getType().equals(UserStateType.RTC_GROUP.getCode())) {
                // 强制退出通话
                webrtcGroupService.forceQuit((Long)state.getData());
            }
        } catch (Exception e) {
            log.error("强制挂断通话异常", e);
        }

    }
}
