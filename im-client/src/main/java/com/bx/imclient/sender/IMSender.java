package com.bx.imclient.sender;

import cn.hutool.core.collection.CollUtil;
import com.bx.imclient.listener.MessageListenerMulticaster;
import com.bx.imcommon.contant.ChatRedisKey;
import com.bx.imcommon.enums.IMCmdType;
import com.bx.imcommon.enums.IMListenerType;
import com.bx.imcommon.enums.IMSendCode;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.*;
import com.bx.imcommon.mq.RedisMQTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IMSender {

    @Autowired
    private RedisMQTemplate redisMQTemplate;

    @Value("${spring.application.name}")
    private String appName;

    private final MessageListenerMulticaster listenerMulticaster;

    public <T> void sendSystemMessage(IMSystemMessage<T> message) {
        // 按 IM-server 分组；WEB 多设备按 deviceId 展开
        Map<String, IMUserInfo> sendMap = new HashMap<>();
        List<IMUserInfo> offLineUsers = new LinkedList<>();
        for (Integer terminal : message.getRecvTerminals()) {
            for (Long id : message.getRecvIds()) {
                if (IMTerminalType.APP.code().equals(terminal)) {
                    String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, id.toString(), terminal.toString());
                    sendMap.put(key, new IMUserInfo(id, terminal));
                } else {
                    String pattern = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, id.toString(), terminal.toString(), "*");
                    Set<String> keys = redisMQTemplate.keys(pattern);
                    if (keys != null && !keys.isEmpty()) {
                        for (String k : keys) {
                            Object sid = redisMQTemplate.opsForValue().get(k);
                            if (sid == null) {
                                continue;
                            }
                            String deviceId = k.contains(":") ? k.substring(k.lastIndexOf(':') + 1) : "";
                            if ("default".equals(deviceId)) {
                                deviceId = "";
                            }
                            IMUserInfo u = new IMUserInfo(id, terminal);
                            u.setDeviceId(deviceId);
                            sendMap.put(k, u);
                        }
                    } else {
                        offLineUsers.add(new IMUserInfo(id, terminal));
                    }
                }
            }
        }
        if (sendMap.isEmpty()) {
            if (message.getSendResult() && !offLineUsers.isEmpty()) {
                List<IMSendResult> results = new LinkedList<>();
                for (IMUserInfo u : offLineUsers) {
                    IMSendResult r = new IMSendResult();
                    r.setReceiver(u);
                    r.setCode(IMSendCode.NOT_ONLINE.code());
                    r.setData(message.getData());
                    results.add(r);
                }
                listenerMulticaster.multicast(IMListenerType.SYSTEM_MESSAGE, results);
            }
            return;
        }
        List<Object> serverIds = redisMQTemplate.opsForValue().multiGet(sendMap.keySet());
        Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>();
        int idx = 0;
        for (Map.Entry<String, IMUserInfo> entry : sendMap.entrySet()) {
            Object sid = idx < serverIds.size() ? serverIds.get(idx) : null;
            idx++;
            Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
            if (serverId != null) {
                serverMap.computeIfAbsent(serverId, o -> new LinkedList<>()).add(entry.getValue());
            } else {
                offLineUsers.add(entry.getValue());
            }
        }
        for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
            IMRecvInfo recvInfo = new IMRecvInfo();
            recvInfo.setCmd(IMCmdType.SYSTEM_MESSAGE.code());
            recvInfo.setReceivers(new LinkedList<>(entry.getValue()));
            recvInfo.setServiceName(appName);
            recvInfo.setSendResult(message.getSendResult());
            recvInfo.setData(message.getData());
            String key = String.join(":", ChatRedisKey.IM_MESSAGE_SYSTEM_QUEUE, entry.getKey().toString());
            redisMQTemplate.opsForList().rightPush(key, recvInfo);
        }
        if (message.getSendResult() && !offLineUsers.isEmpty()) {
            List<IMSendResult> results = new LinkedList<>();
            for (IMUserInfo offLineUser : offLineUsers) {
                IMSendResult result = new IMSendResult();
                result.setReceiver(offLineUser);
                result.setCode(IMSendCode.NOT_ONLINE.code());
                result.setData(message.getData());
                results.add(result);
            }
            listenerMulticaster.multicast(IMListenerType.SYSTEM_MESSAGE, results);
        }
    }

    public <T> void sendPrivateMessage(IMPrivateMessage<T> message) {
        List<IMSendResult> results = new LinkedList<>();
        if (!Objects.isNull(message.getRecvId())) {
            for (Integer terminal : message.getRecvTerminals()) {
                if (IMTerminalType.APP.code().equals(terminal)) {
                    // APP 单设备：单 key
                    String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, message.getRecvId().toString(),
                        terminal.toString());
                    Object sid = redisMQTemplate.opsForValue().get(key);
                    Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
                    if (serverId != null) {
                        pushPrivateToServer(message, serverId, Collections.singletonList(
                            new IMUserInfo(message.getRecvId(), terminal)), message.getSendResult());
                    } else {
                        results.add(makeOfflineResult(message.getSender(), message.getRecvId(), terminal, message.getData()));
                    }
                } else {
                    // WEB 多设备：枚举 chat:user:server_id:{recvId}:0:* 按 serverId 分组投递
                    String pattern = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, message.getRecvId().toString(),
                        terminal.toString(), "*");
                    Set<String> keys = redisMQTemplate.keys(pattern);
                    if (keys != null && !keys.isEmpty()) {
                        Map<Integer, List<IMUserInfo>> serverToReceivers = new HashMap<>();
                        for (String k : keys) {
                            Object sid = redisMQTemplate.opsForValue().get(k);
                            Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
                            if (serverId == null) {
                                continue;
                            }
                            String deviceId = k.contains(":") ? k.substring(k.lastIndexOf(':') + 1) : "";
                            if ("default".equals(deviceId)) {
                                deviceId = "";
                            }
                            IMUserInfo recv = new IMUserInfo(message.getRecvId(), terminal);
                            recv.setDeviceId(deviceId);
                            serverToReceivers.computeIfAbsent(serverId, o -> new LinkedList<>()).add(recv);
                        }
                        for (Map.Entry<Integer, List<IMUserInfo>> e : serverToReceivers.entrySet()) {
                            pushPrivateToServer(message, e.getKey(), e.getValue(), message.getSendResult());
                        }
                    } else {
                        results.add(makeOfflineResult(message.getSender(), message.getRecvId(), terminal, message.getData()));
                    }
                }
            }
        }

        // 推送给自己的其他终端（多设备时每个设备一份）
        if (message.getSendToSelf()) {
            for (Integer terminal : IMTerminalType.codes()) {
                if (message.getSender().getTerminal().equals(terminal)) {
                    continue;
                }
                if (IMTerminalType.APP.code().equals(terminal)) {
                    String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, message.getSender().getId().toString(),
                        terminal.toString());
                    Object sid = redisMQTemplate.opsForValue().get(key);
                    Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
                    if (serverId != null) {
                        pushPrivateToServer(message, serverId,
                            Collections.singletonList(new IMUserInfo(message.getSender().getId(), terminal)), false);
                    }
                } else {
                    String pattern = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, message.getSender().getId().toString(),
                        terminal.toString(), "*");
                    Set<String> keys = redisMQTemplate.keys(pattern);
                    if (keys != null && !keys.isEmpty()) {
                        Map<Integer, List<IMUserInfo>> serverToReceivers = new HashMap<>();
                        for (String k : keys) {
                            Object sid = redisMQTemplate.opsForValue().get(k);
                            Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
                            if (serverId == null) {
                                continue;
                            }
                            String deviceId = k.contains(":") ? k.substring(k.lastIndexOf(':') + 1) : "";
                            if ("default".equals(deviceId)) {
                                deviceId = "";
                            }
                            IMUserInfo recv = new IMUserInfo(message.getSender().getId(), terminal);
                            recv.setDeviceId(deviceId);
                            serverToReceivers.computeIfAbsent(serverId, o -> new LinkedList<>()).add(recv);
                        }
                        for (Map.Entry<Integer, List<IMUserInfo>> e : serverToReceivers.entrySet()) {
                            pushPrivateToServer(message, e.getKey(), e.getValue(), false);
                        }
                    }
                }
            }
        }
        if (message.getSendResult() && !results.isEmpty()) {
            listenerMulticaster.multicast(IMListenerType.PRIVATE_MESSAGE, results);
        }
    }

    private <T> void pushPrivateToServer(IMPrivateMessage<T> message, Integer serverId, List<IMUserInfo> receivers,
        boolean sendResult) {
        String sendKey = String.join(":", ChatRedisKey.IM_MESSAGE_PRIVATE_QUEUE, serverId.toString());
        IMRecvInfo recvInfo = new IMRecvInfo();
        recvInfo.setCmd(IMCmdType.PRIVATE_MESSAGE.code());
        recvInfo.setSendResult(sendResult);
        recvInfo.setServiceName(appName);
        recvInfo.setSender(message.getSender());
        recvInfo.setReceivers(receivers);
        recvInfo.setData(message.getData());
        redisMQTemplate.opsForList().rightPush(sendKey, recvInfo);
    }

    private <T> IMSendResult<Object> makeOfflineResult(IMUserInfo sender, Long recvId, Integer terminal, T data) {
        IMSendResult<Object> result = new IMSendResult<>();
        result.setSender(sender);
        result.setReceiver(new IMUserInfo(recvId, terminal));
        result.setCode(IMSendCode.NOT_ONLINE.code());
        result.setData(data);
        return result;
    }

    public <T> void sendGroupMessage(IMGroupMessage<T> message) {
        // 根据群聊每个成员所连的 IM-server 分组；WEB 多设备按 deviceId 展开
        Map<String, IMUserInfo> sendMap = new HashMap<>();
        List<IMUserInfo> offLineUsers = new LinkedList<>();
        for (Integer terminal : message.getRecvTerminals()) {
            for (Long id : message.getRecvIds()) {
                if (IMTerminalType.APP.code().equals(terminal)) {
                    String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, id.toString(), terminal.toString());
                    sendMap.put(key, new IMUserInfo(id, terminal));
                } else {
                    String pattern = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, id.toString(), terminal.toString(), "*");
                    Set<String> keys = redisMQTemplate.keys(pattern);
                    if (keys != null && !keys.isEmpty()) {
                        for (String k : keys) {
                            Object sid = redisMQTemplate.opsForValue().get(k);
                            if (sid == null) {
                                continue;
                            }
                            String deviceId = k.contains(":") ? k.substring(k.lastIndexOf(':') + 1) : "";
                            if ("default".equals(deviceId)) {
                                deviceId = "";
                            }
                            IMUserInfo u = new IMUserInfo(id, terminal);
                            u.setDeviceId(deviceId);
                            sendMap.put(k, u);
                        }
                    } else {
                        offLineUsers.add(new IMUserInfo(id, terminal));
                    }
                }
            }
        }
        List<Object> serverIds = sendMap.isEmpty() ? Collections.emptyList() : redisMQTemplate.opsForValue().multiGet(sendMap.keySet());
        Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>();
        int idx = 0;
        for (Map.Entry<String, IMUserInfo> entry : sendMap.entrySet()) {
            Object sid = serverIds != null && idx < serverIds.size() ? serverIds.get(idx) : null;
            idx++;
            Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
            if (serverId != null) {
                serverMap.computeIfAbsent(serverId, o -> new LinkedList<>()).add(entry.getValue());
            } else {
                offLineUsers.add(entry.getValue());
            }
        }
        for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
            IMRecvInfo recvInfo = new IMRecvInfo();
            recvInfo.setCmd(IMCmdType.GROUP_MESSAGE.code());
            recvInfo.setReceivers(new LinkedList<>(entry.getValue()));
            recvInfo.setSender(message.getSender());
            recvInfo.setServiceName(appName);
            recvInfo.setSendResult(message.getSendResult());
            recvInfo.setData(message.getData());
            String key = String.join(":", ChatRedisKey.IM_MESSAGE_GROUP_QUEUE, entry.getKey().toString());
            redisMQTemplate.opsForList().rightPush(key, recvInfo);
        }

        if (message.getSendToSelf()) {
            for (Integer terminal : IMTerminalType.codes()) {
                if (terminal.equals(message.getSender().getTerminal())) {
                    continue;
                }
                if (IMTerminalType.APP.code().equals(terminal)) {
                    String key = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, message.getSender().getId().toString(),
                        terminal.toString());
                    Object sid = redisMQTemplate.opsForValue().get(key);
                    Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
                    if (serverId != null) {
                        pushGroupToServer(message, serverId, Collections.singletonList(
                            new IMUserInfo(message.getSender().getId(), terminal)));
                    }
                } else {
                    String pattern = String.join(":", ChatRedisKey.IM_USER_SERVER_ID, message.getSender().getId().toString(),
                        terminal.toString(), "*");
                    Set<String> keys = redisMQTemplate.keys(pattern);
                    if (keys != null && !keys.isEmpty()) {
                        Map<Integer, List<IMUserInfo>> byServer = new HashMap<>();
                        for (String k : keys) {
                            Object sid = redisMQTemplate.opsForValue().get(k);
                            Integer serverId = sid instanceof Number ? ((Number) sid).intValue() : null;
                            if (serverId == null) {
                                continue;
                            }
                            String deviceId = k.contains(":") ? k.substring(k.lastIndexOf(':') + 1) : "";
                            if ("default".equals(deviceId)) {
                                deviceId = "";
                            }
                            IMUserInfo u = new IMUserInfo(message.getSender().getId(), terminal);
                            u.setDeviceId(deviceId);
                            byServer.computeIfAbsent(serverId, o -> new LinkedList<>()).add(u);
                        }
                        for (Map.Entry<Integer, List<IMUserInfo>> e : byServer.entrySet()) {
                            pushGroupToServer(message, e.getKey(), e.getValue());
                        }
                    }
                }
            }
        }
        if (message.getSendResult() && !offLineUsers.isEmpty()) {
            List<IMSendResult> results = new LinkedList<>();
            for (IMUserInfo offLineUser : offLineUsers) {
                IMSendResult result = new IMSendResult();
                result.setSender(message.getSender());
                result.setReceiver(offLineUser);
                result.setCode(IMSendCode.NOT_ONLINE.code());
                result.setData(message.getData());
                results.add(result);
            }
            listenerMulticaster.multicast(IMListenerType.GROUP_MESSAGE, results);
        }
    }

    private <T> void pushGroupToServer(IMGroupMessage<T> message, Integer serverId, List<IMUserInfo> receivers) {
        IMRecvInfo recvInfo = new IMRecvInfo();
        recvInfo.setCmd(IMCmdType.GROUP_MESSAGE.code());
        recvInfo.setSender(message.getSender());
        recvInfo.setReceivers(receivers);
        recvInfo.setSendResult(false);
        recvInfo.setData(message.getData());
        String sendKey = String.join(":", ChatRedisKey.IM_MESSAGE_GROUP_QUEUE, serverId.toString());
        redisMQTemplate.opsForList().rightPush(sendKey, recvInfo);
    }

    public Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        Map<Long, List<IMTerminalType>> onlineMap = new HashMap<>();
        for (Long id : userIds) {
            List<IMTerminalType> terminals = new LinkedList<>();
            if (redisMQTemplate.hasKey(String.join(":", ChatRedisKey.IM_USER_SERVER_ID, id.toString(),
                IMTerminalType.APP.code().toString()))) {
                terminals.add(IMTerminalType.APP);
            }
            Set<String> webKeys = redisMQTemplate.keys(String.join(":", ChatRedisKey.IM_USER_SERVER_ID, id.toString(),
                IMTerminalType.WEB.code().toString(), "*"));
            if (webKeys != null && !webKeys.isEmpty()) {
                terminals.add(IMTerminalType.WEB);
            }
            if (!terminals.isEmpty()) {
                onlineMap.put(id, terminals);
            }
        }
        return onlineMap;
    }

    public List<IMTerminalType> getOnlineTerminal(Long userId) {
        List<IMTerminalType> terminals = new LinkedList<>();
        for (Integer terminal : IMTerminalType.codes()) {
            IMTerminalType type = IMTerminalType.fromCode(terminal);
            if (isOnline(userId, type)) {
                terminals.add(type);
            }
        }
        return terminals;
    }

    public Boolean isOnline(Long userId, IMTerminalType terminal) {
        if (IMTerminalType.APP.equals(terminal)) {
            return redisMQTemplate.hasKey(String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(), terminal.code().toString()));
        }
        Set<String> keys = redisMQTemplate.keys(String.join(":", ChatRedisKey.IM_USER_SERVER_ID, userId.toString(),
            terminal.code().toString(), "*"));
        return keys != null && !keys.isEmpty();
    }

    public Boolean isOnline(Long userId) {
        if (Boolean.TRUE.equals(isOnline(userId, IMTerminalType.APP)) || Boolean.TRUE.equals(isOnline(userId, IMTerminalType.WEB))) {
            return true;
        }
        return false;
    }

    public List<Long> getOnlineUser(List<Long> userIds) {
        return new LinkedList<>(getOnlineTerminal(userIds).keySet());
    }
}
