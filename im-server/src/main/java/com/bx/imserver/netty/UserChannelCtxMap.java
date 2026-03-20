package com.bx.imserver.netty;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户连接映射：userId -> terminal -> deviceId -> ctx
 * WEB(0) 支持同 terminal 多 deviceId；APP(1) 仅保留单连接（单 deviceId）
 */
public class UserChannelCtxMap {

    /** userId -> (terminal -> (deviceId -> ctx)) */
    private static final Map<Long, Map<Integer, Map<String, ChannelHandlerContext>>> CHANNEL_MAP = new ConcurrentHashMap<>();

    private static String normalizeDeviceId(String deviceId) {
        return deviceId == null || deviceId.isEmpty() ? "" : deviceId;
    }

    public static void addChannelCtx(Long userId, Integer terminal, ChannelHandlerContext ctx) {
        addChannelCtx(userId, terminal, null, ctx);
    }

    public static void addChannelCtx(Long userId, Integer terminal, String deviceId, ChannelHandlerContext ctx) {
        String did = normalizeDeviceId(deviceId);
        CHANNEL_MAP
            .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(terminal, k -> new ConcurrentHashMap<>())
            .put(did, ctx);
    }

    public static void removeChannelCtx(Long userId, Integer terminal) {
        removeChannelCtx(userId, terminal, null);
    }

    public static void removeChannelCtx(Long userId, Integer terminal, String deviceId) {
        if (userId == null || terminal == null) {
            return;
        }
        Map<Integer, Map<String, ChannelHandlerContext>> byTerminal = CHANNEL_MAP.get(userId);
        if (byTerminal == null) {
            return;
        }
        Map<String, ChannelHandlerContext> byDevice = byTerminal.get(terminal);
        if (byDevice != null) {
            byDevice.remove(normalizeDeviceId(deviceId));
            if (byDevice.isEmpty()) {
                byTerminal.remove(terminal);
            }
        }
        if (byTerminal.isEmpty()) {
            CHANNEL_MAP.remove(userId);
        }
    }

    public static ChannelHandlerContext getChannelCtx(Long userId, Integer terminal) {
        return getChannelCtx(userId, terminal, null);
    }

    public static ChannelHandlerContext getChannelCtx(Long userId, Integer terminal, String deviceId) {
        if (userId == null || terminal == null) {
            return null;
        }
        Map<Integer, Map<String, ChannelHandlerContext>> byTerminal = CHANNEL_MAP.get(userId);
        if (byTerminal == null) {
            return null;
        }
        Map<String, ChannelHandlerContext> byDevice = byTerminal.get(terminal);
        if (byDevice == null) {
            return null;
        }
        String did = normalizeDeviceId(deviceId);
        if (byDevice.containsKey(did)) {
            return byDevice.get(did);
        }
        // 兼容：未传 deviceId 时，若该 terminal 仅有一个连接则返回（APP 单设备）
        if (byDevice.size() == 1) {
            return byDevice.values().iterator().next();
        }
        return null;
    }
}
