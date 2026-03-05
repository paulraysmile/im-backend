package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.bx.imcommon.model.IMSendResult;
import com.bx.implatform.annotation.NotifyCheck;
import com.bx.implatform.config.props.NotifyProperties;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.service.FriendService;
import com.bx.implatform.service.GroupService;
import com.bx.implatform.service.OfflineNotifyService;
import com.bx.implatform.session.OfflineNotifySession;
import com.bx.implatform.thirdparty.UniPushService;
import com.bx.implatform.vo.FriendRequestVO;
import com.bx.implatform.vo.GroupMessageVO;
import com.bx.implatform.vo.PrivateMessageVO;
import com.bx.implatform.vo.SystemMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推送离线通知
 * author: blue date: 2024-08-23 version: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineNotifyServiceImpl implements OfflineNotifyService {

    private final UniPushService uniPushService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotifyProperties notifyProps;
    private final FriendService friendService;
    private final GroupService groupService;

    @NotifyCheck
    @Override
    public void sendPrivateOfflineNotify(List<IMSendResult<PrivateMessageVO>> results) {
        // 接收用户列表
        Set<Long> recvIds = results.stream().map(r -> r.getReceiver().getId()).collect(Collectors.toSet());
        // Map<接受用户id,cid>
        Map<Long, String> cidMap = getCId(recvIds);
        // Map<接受用户id,session>
        Map<Long, OfflineNotifySession> sessionMap = findNotifySession(cidMap.keySet());
        results.forEach(ret -> {
            Long sendId = ret.getSender().getId();
            Long recvId = ret.getReceiver().getId();
            String cid = cidMap.get(recvId);
            // 没有cid，则不推送
            if (StrUtil.isEmpty(cid)) {
                return;
            }
            // 通知会话信息
            OfflineNotifySession session = sessionMap.get(recvId);
            // 已达到最大数量，则不推送
            if (notifyProps.getMaxSize() > 0 && !Objects.isNull(
                session) && session.getMessageSize() >= notifyProps.getMaxSize()) {
                log.info("用户'{}'已到达推送数量上线,不再推送离线通知", recvId);
                sessionMap.remove(recvId);
                return;
            }
            // 接收方开启免打扰，则不推送
            if (friendService.isDnd(recvId, sendId)) {
                return;
            }
            if (Objects.isNull(session)) {
                session = createEmptySession(recvId);
                sessionMap.put(recvId, session);
            }
            session.getFriendIds().add(sendId);
            session.setMessageSize(session.getMessageSize() + 1);
            // 推送离线通知
            sendNotifyMessage(recvId, cid, session);
        });
        // 保存session
        saveNotifySession(sessionMap);
    }

    @NotifyCheck
    @Override
    public void sendGroupOfflineNotify(List<IMSendResult<GroupMessageVO>> results) {
        // 接收用户列表
        Set<Long> recvIds = results.stream().map(r -> r.getReceiver().getId()).collect(Collectors.toSet());
        // Map<接受用户id,cid>
        Map<Long, String> cidMap = getCId(recvIds);
        // 获取离线通知session
        Map<Long, OfflineNotifySession> sessionMap = findNotifySession(cidMap.keySet());
        results.forEach(ret -> {
            Long groupId = ret.getData().getGroupId();
            Long recvId = ret.getReceiver().getId();
            String cid = cidMap.get(recvId);
            // 如果没有cid,则不推送
            if (StrUtil.isEmpty(cid)) {
                return;
            }
            OfflineNotifySession session = sessionMap.get(recvId);
            // 如果已达到最大数量，则不推送
            if (notifyProps.getMaxSize() > 0 && !Objects.isNull(
                session) && session.getMessageSize() >= notifyProps.getMaxSize()) {
                log.info("用户'{}'已到达推送数量上线,不再推送离线通知", recvId);
                sessionMap.remove(recvId);
                return;
            }
            // 如果接收方开启了免打扰，则不推送
            if (groupService.isDnd(recvId, groupId)) {
                return;
            }
            if (Objects.isNull(session)) {
                session = createEmptySession(recvId);
                sessionMap.put(recvId, session);
            }
            session.getGroupIds().add(groupId);
            session.setMessageSize(session.getMessageSize() + 1);
            // 推送离线通知
            sendNotifyMessage(recvId, cid, session);
        });
        // 保存session
        saveNotifySession(sessionMap);
    }

    @NotifyCheck
    @Override
    public void sendSystemOfflineNotify(List<IMSendResult<SystemMessageVO>> results) {
        // 接收用户列表
        Set<Long> recvIds = results.stream().map(r -> r.getReceiver().getId()).collect(Collectors.toSet());
        // Map<接受用户id,cid>
        Map<Long, String> cidMap = getCId(recvIds);
        results.forEach(ret -> {
            SystemMessageVO messageVo = ret.getData();
            Long recvId = ret.getReceiver().getId();
            String cid = cidMap.get(recvId);
            if (StrUtil.isEmpty(cid)) {
                return;
            }
            // 推送
            String notifyId = Math.abs(messageVo.hashCode()) + "";
            uniPushService.asyncSend(cid, "系统通知", messageVo.getTitle(), "", notifyId, messageVo);
        });
    }

    @NotifyCheck
    @Override
    public void sendFriendRequestNotify(PrivateMessageVO messageInfo) {
        if (messageInfo.getType().equals(MessageType.FRIEND_REQ_APPLY.code())) {
            String key = buildCidKey(messageInfo.getRecvId());
            String cid = (String)redisTemplate.opsForValue().get(key);
            if (StrUtil.isEmpty(cid)) {
                return;
            }
            String notifyId = Math.abs(messageInfo.hashCode()) + "";
            FriendRequestVO vo = JSON.parseObject(messageInfo.getContent(), FriendRequestVO.class);
            String body = "请求添加您为好友";
            uniPushService.asyncSend(cid, vo.getSendNickName(), body, vo.getSendHeadImage(), notifyId, messageInfo);
        }
    }

    private void sendNotifyMessage(Long recvId, String cid, OfflineNotifySession session) {
        int linkmanSize = session.getFriendIds().size() + session.getGroupIds().size();
        int messageSize = session.getMessageSize();
        String body = String.format("%s位联系人发来%s条消息", linkmanSize, messageSize);
        String notifyId = Math.abs(buildSessionKey(recvId).hashCode()) + "";
        // 推送
        uniPushService.asyncSend(cid, notifyProps.getAppName(), body, "", notifyId, null);
    }

    /**
     * 批量获取离线通知session
     */
    private Map<Long, OfflineNotifySession> findNotifySession(Set<Long> userIds) {
        List<String> keys = userIds.stream().map(this::buildSessionKey).collect(Collectors.toList());
        List<Object> sessions = redisTemplate.opsForValue().multiGet(keys);
        Map<Long, OfflineNotifySession> sessionMap = new HashMap<>();
        int idx = 0;
        for (Long userId : userIds) {
            Object session = sessions.get(idx++);
            if (!Objects.isNull(session)) {
                sessionMap.put(userId, (OfflineNotifySession)session);
            }
        }
        return sessionMap;
    }

    /**
     * 批量获取用户cid
     */
    private Map<Long, String> getCId(Set<Long> userIds) {
        List<String> keys = userIds.stream().map(this::buildCidKey).collect(Collectors.toList());
        List<Object> cids = redisTemplate.opsForValue().multiGet(keys);
        Map<Long, String> cidMap = new HashMap<>();
        int idx = 0;
        for (Long userId : userIds) {
            Object cid = cids.get(idx++);
            if (!Objects.isNull(cid)) {
                cidMap.put(userId, cid.toString());
            }
        }
        return cidMap;
    }

    /**
     * 批量保存session
     */
    private void saveNotifySession(Map<Long, OfflineNotifySession> sessionMap) {
        Map<String, OfflineNotifySession> map = sessionMap.entrySet().stream()
            .collect(Collectors.toMap(entry -> buildSessionKey(entry.getKey()), Map.Entry::getValue));
        redisTemplate.opsForValue().multiSet(map);
    }

    /**
     * 创建一个空会话
     */
    private OfflineNotifySession createEmptySession(Long userId) {
        OfflineNotifySession session = new OfflineNotifySession();
        String key = buildSessionKey(userId);
        redisTemplate.opsForValue().set(key, session, notifyProps.getActiveDays(), TimeUnit.DAYS);
        return session;
    }

    private String buildSessionKey(Long userId) {
        return StrUtil.join(":", RedisKey.IM_NOTIFY_OFFLINE_SESSION, userId);
    }

    private String buildCidKey(Long userId) {
        return StrUtil.join(":", RedisKey.IM_USER_CID, userId);
    }
}
