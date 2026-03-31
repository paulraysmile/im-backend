package com.bx.imserver.netty;

import cn.hutool.core.collection.CollectionUtil;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserChannelCtxMap {

    private static Map<Long, ChannelHandlerContext> appChannelMap = new ConcurrentHashMap();

    private static Map<Long, List<ChannelHandlerContext>> webChannelMap = new ConcurrentHashMap();

    public static void addAppChannelCtx(Long userId, ChannelHandlerContext ctx) {
        appChannelMap.put(userId, ctx);
    }

    public static void addWebChannelCtx(Long userId, ChannelHandlerContext ctx) {
        webChannelMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(ctx);
    }

    public static void removeAppChannelCtx(Long userId) {
        if (userId == null) {
            return;
        }
        appChannelMap.remove(userId);
    }

    public static void removeWebChannelCtx(Long userId, Integer terminal, ChannelHandlerContext ctx) {
        if (userId == null ||  terminal == null) {
            return;
        }
        Map<Integer, List<ChannelHandlerContext>> userChannelMap = webChannelMap.get(userId);
        if (CollectionUtil.isNotEmpty(userChannelMap)) {
            if (ctx == null) {
                userChannelMap.remove(terminal);
            } else {
                List<ChannelHandlerContext> channelHandlerContexts = userChannelMap.get(terminal);
                channelHandlerContexts.removeIf(context -> ctx.channel().id().equals(context.channel().id()));
                if (CollectionUtil.isEmpty(channelHandlerContexts)) {
                    userChannelMap.remove(terminal);
                    if (CollectionUtil.isEmpty(userChannelMap)) {
                        webChannelMap.remove(userId);
                    }
                }
            }
        }
    }

    public static ChannelHandlerContext getAppChannelCtx(Long userId) {
        if (userId == null) {
            return null;
        }
        return appChannelMap.get(userId);
    }

    public static List<ChannelHandlerContext> getWebChannelCtx(Long userId, Integer terminal) {
        if (userId == null ||  terminal == null) {
            return null;
        }
        Map<Integer, List<ChannelHandlerContext>> userChannelMap = webChannelMap.get(userId);
        if (CollectionUtil.isEmpty(userChannelMap)) {
            return null;
        }
        return userChannelMap.get(terminal);
    }

}
