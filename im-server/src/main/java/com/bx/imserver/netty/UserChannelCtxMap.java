package com.bx.imserver.netty;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserChannelCtxMap {

    private static Map<Long, Map<Integer, List<ChannelHandlerContext>>> channelMap = new ConcurrentHashMap();

    public static void addChannelCtx(Long userId, Integer terminal, ChannelHandlerContext ctx) {
        channelMap.computeIfAbsent(userId, key -> new ConcurrentHashMap()).computeIfAbsent(terminal, k -> new CopyOnWriteArrayList<>()).add(ctx);
    }

    public static void removeChannelCtx(Long userId, Integer terminal, String channelId) {
        if (userId != null && terminal != null && channelMap.containsKey(userId)) {
            Map<Integer, List<ChannelHandlerContext>> userChannelMap = channelMap.get(userId);
            if (userChannelMap.containsKey(terminal)) {



            }


            userChannelMap.remove(terminal);
            if (userChannelMap.isEmpty()) {
                channelMap.remove(userId);
            }
        }
    }

    public static ChannelHandlerContext getChannelCtx(Long userId, Integer terminal) {
        if (userId != null && terminal != null && channelMap.containsKey(userId)) {
            Map<Integer, ChannelHandlerContext> userChannelMap = channelMap.get(userId);
            if (userChannelMap.containsKey(terminal)) {
                return userChannelMap.get(terminal);
            }
        }
        return null;
    }

}
