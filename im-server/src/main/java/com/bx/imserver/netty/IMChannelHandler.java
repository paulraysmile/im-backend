package com.bx.imserver.netty;

import cn.hutool.core.collection.CollectionUtil;
import com.bx.imcommon.contant.ChatRedisKey;
import com.bx.imcommon.enums.IMCmdType;
import com.bx.imcommon.enums.IMEventType;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMSendInfo;
import com.bx.imcommon.model.IMUserEvent;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.imcommon.mq.RedisMQTemplate;
import com.bx.imserver.constant.ChannelAttrKey;
import com.bx.imserver.netty.processor.AbstractMessageProcessor;
import com.bx.imserver.netty.processor.ProcessorFactory;
import com.bx.imserver.util.SpringContextHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * WebSocket 长连接下 文本帧的处理器
 * 实现浏览器发送文本回写
 * 浏览器连接状态监控
 */
@Slf4j
public class IMChannelHandler extends SimpleChannelInboundHandler<IMSendInfo> {

    /**
     * 读取到消息后进行处理
     *
     * @param ctx      channel上下文
     * @param sendInfo 发送消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IMSendInfo sendInfo) {
        // 创建处理器进行处理
        AbstractMessageProcessor processor = ProcessorFactory.createProcessor(IMCmdType.fromCode(sendInfo.getCmd()));
        processor.process(ctx, processor.transForm(sendInfo.getData()));
    }

    /**
     * 出现异常的处理 打印报错日志
     *
     * @param ctx   channel上下文
     * @param cause 异常信息
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage());
        //关闭上下文
        //ctx.close();
    }

    /**
     * 监控浏览器上线
     *
     * @param ctx channel上下文
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info(ctx.channel().id().asLongText() + "连接");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        AttributeKey<Long> userIdAttr = AttributeKey.valueOf(ChannelAttrKey.USER_ID);
        Long userId = channel.attr(userIdAttr).get();
        AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
        Integer terminal = channel.attr(terminalAttr).get();
        RedisMQTemplate redisTemplate = SpringContextHolder.getBean(RedisMQTemplate.class);
        if (IMTerminalType.APP.code().equals(terminal)) {
            ChannelHandlerContext context = UserChannelCtxMap.getAppChannelCtx(userId);
            // 判断一下，避免异地登录导致的误删
            if (context != null && ctx.channel().id().equals(context.channel().id())) {
                // 移除channel
                UserChannelCtxMap.removeAppChannelCtx(userId);
                // 用户下线
                redisTemplate.delete(String.join(":", ChatRedisKey.IM_USER_APP_SERVER_ID, userId.toString(), terminal.toString()));
            }
        } else {
            List<ChannelHandlerContext> contexts = UserChannelCtxMap.getWebChannelCtx(userId, terminal);
            if (CollectionUtil.isEmpty(contexts)) {
                return;
            }
            contexts.removeIf(context -> ctx.channel().id().equals(context.channel().id()));
            // 用户下线
            String key = String.join(":", ChatRedisKey.IM_USER_WEB_SERVER_COUNT, userId.toString(), terminal.toString(), String.valueOf(IMServerGroup.serverId));
            Long decremented = redisTemplate.opsForValue().decrement(key);
            if (decremented <= 0) {
                redisTemplate.delete(key);
                key = String.join(":", ChatRedisKey.IM_USER_WEB_SERVER_ID, userId.toString(), terminal.toString());
                redisTemplate.opsForSet().remove(key, IMServerGroup.serverId);
                Long size = redisTemplate.opsForSet().size(key);
                if (size <= 0) {
                    redisTemplate.delete(key);
                }
            }
        }
        // 推送用户下线事件给业务层
        IMUserEvent event = new IMUserEvent();
        event.setEventType(IMEventType.OFFLINE.code());
        event.setUserInfo(new IMUserInfo(userId,terminal));
        redisTemplate.opsForList().rightPush(ChatRedisKey.IM_USER_EVENT_QUEUE, event);
        log.info("断开连接,userId:{},终端类型:{},{}", userId, terminal, ctx.channel().id().asLongText());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                // 在规定时间内没有收到客户端的上行数据, 主动断开连接
                AttributeKey<Long> attr = AttributeKey.valueOf("USER_ID");
                Long userId = ctx.channel().attr(attr).get();
                AttributeKey<Integer> terminalAttr = AttributeKey.valueOf(ChannelAttrKey.TERMINAL_TYPE);
                Integer terminal = ctx.channel().attr(terminalAttr).get();
                log.info("心跳超时，即将断开连接,用户id:{},终端类型:{} ", userId, terminal);
                ctx.channel().close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }

    }
}