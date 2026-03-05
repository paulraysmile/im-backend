package com.bx.implatform.service;

import com.bx.imcommon.model.IMSendResult;
import com.bx.implatform.vo.GroupMessageVO;
import com.bx.implatform.vo.PrivateMessageVO;
import com.bx.implatform.vo.SystemMessageVO;

import java.util.List;

/**
 * @author: Blue
 * @date: 2024-08-24
 * @version: 1.0
 */
public interface OfflineNotifyService {

    /**
     * 推送私聊离线通知
     *
     * @param results 消息推送结果
     */
    void sendPrivateOfflineNotify(List<IMSendResult<PrivateMessageVO>> results);

    /**
     * 推送群聊离线通知
     *
     * @param results 消息推送结果
     */
    void sendGroupOfflineNotify(List<IMSendResult<GroupMessageVO>> results);

    /**
     * 推送系统离线通知
     *
     * @param results 消息推送结果
     */
    void sendSystemOfflineNotify(List<IMSendResult<SystemMessageVO>> results);

    /**
     * 推送好友请求消息
     *
     * @param messageInfo    推送消息
     */
    void sendFriendRequestNotify(PrivateMessageVO messageInfo);
}
