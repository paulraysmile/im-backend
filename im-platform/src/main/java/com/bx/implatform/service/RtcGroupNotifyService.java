package com.bx.implatform.service;

import com.bx.implatform.entity.Group;
import com.bx.implatform.session.WebrtcUserInfo;

import java.util.List;

public interface RtcGroupNotifyService {

    /**
     * 发起通话呼叫
     *
     * @param group   群聊信息
     * @param inviter 发起邀请人信息
     * @param recvIds 被邀请人id
     */
    void setUp(Group group, WebrtcUserInfo inviter, List<Long> recvIds);

    /**
     * 停止通话呼叫
     *
     * @param groupId 群聊id
     * @param recvIds 被邀请人id
     * @param tip     通知提示
     */
    void stop(Long groupId, List<Long> recvIds, String tip);

    /**
     * 停止通话呼叫
     *
     * @param groupId 群聊id
     * @param recvId  被邀请人id
     * @param tip     通知提示
     */
    void stop(Long groupId, Long recvId, String tip);

}
