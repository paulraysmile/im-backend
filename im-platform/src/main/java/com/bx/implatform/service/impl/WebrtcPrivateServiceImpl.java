package com.bx.implatform.service.impl;

import com.bx.imclient.IMClient;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMPrivateMessage;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.imcommon.util.ThreadPoolExecutorFactory;
import com.bx.implatform.annotation.OnlineCheck;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.PrivateMessage;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.enums.WebrtcMode;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.service.*;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.session.WebrtcPrivateSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.util.UserStateUtils;
import com.bx.implatform.vo.PrivateMessageVO;
import com.bx.implatform.vo.WebrtcPrivateInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebrtcPrivateServiceImpl implements WebrtcPrivateService {

    private final IMClient imClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PrivateMessageService privateMessageService;
    private final RtcPrivateNotifyService rtcPrivateNotifyService;
    private final UserBlacklistService userBlacklistService;
    private final UserStateUtils userStateUtils;
    private final FriendService friendService;

    @OnlineCheck
    @Override
    public void setup(Long uid, String mode) {
        UserSession session = SessionContext.getSession();
        if (!friendService.isFriend(session.getUserId(), uid)) {
            throw new GlobalException("您已不是对方好友，无法呼叫");
        }
        if (userBlacklistService.isInBlacklist(uid, session.getUserId())) {
            throw new GlobalException("对方已将您拉入黑名单，呼叫失败");
        }
        // 创建webrtc会话
        WebrtcPrivateSession webrtcSession = new WebrtcPrivateSession();
        webrtcSession.setChatId(RandomUtils.nextLong());
        webrtcSession.setHost(new IMUserInfo(session.getUserId(), session.getTerminal()));
        webrtcSession.setAcceptor(new IMUserInfo(uid, IMTerminalType.UNKNOW.code()));
        webrtcSession.setMode(mode);
        if (userStateUtils.isBusy(uid)) {
            this.sendActMessage(webrtcSession, MessageStatus.PENDING, "未接通");
            throw new GlobalException("对方正忙,请稍后重试");
        }
        // 保存rtc session
        String key = getWebRtcSessionKey(session.getUserId(), uid);
        redisTemplate.opsForValue().set(key, webrtcSession, 60, TimeUnit.SECONDS);
        // 标记用户忙线状态
        userStateUtils.inRrivateRtc(session.getUserId(), uid);
        userStateUtils.inRrivateRtc(uid, session.getUserId());
        // 向对方所有终端发起呼叫
        MessageType messageType =
            mode.equals(WebrtcMode.VIDEO.getValue()) ? MessageType.RTC_SETUP_VIDEO : MessageType.RTC_SETUP_VOICE;
        sendRtcMessage1(messageType, uid);
        // 对离线用户进行离线呼叫
        rtcPrivateNotifyService.setup(webrtcSession);
        // 超时未接听检测
        detectTimeout(webrtcSession.getChatId(), session.getUserId(), uid);
        log.info("发起呼叫,sid:{},uid:{}", session.getUserId(), uid);
    }

    @Override
    public void accept(Long uid) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 更新接受者信息
        webrtcSession.setAcceptor(new IMUserInfo(session.getUserId(), session.getTerminal()));
        webrtcSession.setChatTimeStamp(System.currentTimeMillis());
        String key = getWebRtcSessionKey(session.getUserId(), uid);
        redisTemplate.opsForValue().set(key, webrtcSession, 60, TimeUnit.SECONDS);
        // 向发起人推送接受通话信令
        sendRtcMessage2(webrtcSession, MessageType.RTC_ACCEPT, uid, "", true);
        // 停止离线呼叫
        rtcPrivateNotifyService.stop(webrtcSession, "已在其他设备接听");
        log.info("接受通话,sid:{},uid:{}", session.getUserId(), uid);
    }

    @Override
    public void reject(Long uid) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 删除会话信息
        removeWebrtcSession(uid, session.getUserId());
        // 设置用户空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        // 向发起人推送拒绝通话信令
        sendRtcMessage2(webrtcSession, MessageType.RTC_REJECT, uid, "", true);
        // 生成通话消息
        sendActMessage(webrtcSession, MessageStatus.READED, "已拒绝");
        // 停止离线呼叫
        rtcPrivateNotifyService.stop(webrtcSession, "已在其他设备拒绝");
        log.info("拒绝通话,sid:{},uid:{}", session.getUserId(), uid);
    }

    @Override
    public void cancel(Long uid) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 删除会话信息
        removeWebrtcSession(session.getUserId(), uid);
        // 设置用户空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        // 向对方所有终端推送取消通话信令
        sendRtcMessage1(MessageType.RTC_CANCEL, uid);
        // 生成通话消息
        sendActMessage(webrtcSession, MessageStatus.PENDING, "已取消");
        // 停止离线呼叫
        rtcPrivateNotifyService.stop(webrtcSession, "通话已取消");
        log.info("取消通话,sid:{},uid:{}", session.getUserId(), uid);
    }

    @Override
    public void failed(Long uid, String reason) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 删除会话信息
        removeWebrtcSession(uid, session.getUserId());
        // 设置用户空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        // 向发起方推送通话失败信令
        sendRtcMessage2(webrtcSession, MessageType.RTC_FAILED, uid, reason, false);
        // 生成消息
        sendActMessage(webrtcSession, MessageStatus.READED, "未接通");
        log.info("通话失败,sid:{},uid:{},reason:{}", session.getUserId(), uid, reason);
    }

    @Override
    public void handup(Long uid) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 删除会话信息
        removeWebrtcSession(uid, session.getUserId());
        // 设置用户空闲状态
        userStateUtils.setFree(uid);
        userStateUtils.setFree(session.getUserId());
        // 向对方推送挂断通话信令
        sendRtcMessage2(webrtcSession, MessageType.RTC_HANDUP, uid, "", false);
        // 生成通话消息
        sendActMessage(webrtcSession, MessageStatus.READED, "通话时长 " + chatTimeText(webrtcSession));
        log.info("挂断通话,sid:{},uid:{}", session.getUserId(), uid);
    }

    @Override
    public void offer(Long uid, String offer) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 向对方推送offer
        sendRtcMessage2(webrtcSession, MessageType.RTC_OFFER, uid, offer, false);
        log.info("推送offer,sid:{},uid:{}", session.getUserId(), uid);
    }

    @Override
    public void answer(Long uid, String answer) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 推送answer
        sendRtcMessage2(webrtcSession, MessageType.RTC_ANSWER, uid, answer, false);
        log.info("推送answer,sid:{},uid:{}", session.getUserId(), uid);
    }

    @Override
    public void forceQuit(Long uid) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        if (!Objects.isNull(webrtcSession.getChatTimeStamp())) {
            // 通话已开始，挂断
            handup(uid);
        } else if (webrtcSession.getHost().getId().equals(session.getUserId())) {
            // 通话未开始且我是发起人，停止呼叫
            cancel(uid);
        }
    }

    @Override
    public void candidate(Long uid, String candidate) {
        UserSession session = SessionContext.getSession();
        // 查询webrtc会话
        WebrtcPrivateSession webrtcSession = getWebrtcSession(session.getUserId(), uid);
        // 向发起方推送同步candidate信令
        sendRtcMessage2(webrtcSession, MessageType.RTC_CANDIDATE, uid, candidate, false);
    }


    @Override
    public void heartbeat(Long uid) {
        UserSession session = SessionContext.getSession();
        // 会话续命
        String key = getWebRtcSessionKey(session.getUserId(), uid);
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        // 用户状态续命
        userStateUtils.expire(session.getUserId());
    }

    public WebrtcPrivateInfoVO info(Long uid) {
        WebrtcPrivateInfoVO vo = new WebrtcPrivateInfoVO();
        UserSession session = SessionContext.getSession();
        String key = getWebRtcSessionKey(session.getUserId(), uid);
        WebrtcPrivateSession webrtcSession = (WebrtcPrivateSession)redisTemplate.opsForValue().get(key);
        if (Objects.isNull(webrtcSession)) {
            // 通话已结束
            vo.setIsChating(false);
        } else {
            // 正在通话中
            vo.setIsChating(true);
            vo.setHost(webrtcSession.getHost());
            vo.setAcceptor(webrtcSession.getAcceptor());
            vo.setMode(webrtcSession.getMode());
        }
        return vo;
    }

    private WebrtcPrivateSession getWebrtcSession(Long userId, Long uid) {
        String key = getWebRtcSessionKey(userId, uid);
        WebrtcPrivateSession webrtcSession = (WebrtcPrivateSession)redisTemplate.opsForValue().get(key);
        if (Objects.isNull(webrtcSession)) {
            throw new GlobalException("通话已结束");
        }
        return webrtcSession;
    }

    private void removeWebrtcSession(Long userId, Long uid) {
        String key = getWebRtcSessionKey(userId, uid);
        redisTemplate.delete(key);
    }

    private String getWebRtcSessionKey(Long id1, Long id2) {
        Long minId = id1 > id2 ? id2 : id1;
        Long maxId = id1 > id2 ? id1 : id2;
        return String.join(":", RedisKey.IM_WEBRTC_PRIVATE_SESSION, minId.toString(), maxId.toString());
    }

    private Integer getTerminalType(Long uid, WebrtcPrivateSession webrtcSession) {
        if (uid.equals(webrtcSession.getHost().getId())) {
            return webrtcSession.getHost().getTerminal();
        }
        return webrtcSession.getAcceptor().getTerminal();
    }

    /**
     * 发送消息,向对方所有终端广播
     */
    private void sendRtcMessage1(MessageType messageType, Long uid) {
        UserSession session = SessionContext.getSession();
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        messageInfo.setType(messageType.code());
        messageInfo.setRecvId(uid);
        messageInfo.setSendId(session.getUserId());
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(uid);
        sendMessage.setSendToSelf(false);
        sendMessage.setSendResult(false);
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
    }

    /**
     * 发送消息,只发送给通话的终端
     */
    private void sendRtcMessage2(WebrtcPrivateSession rtcSession, MessageType messageType, Long uid, String content,
        Boolean sendToSelf) {
        UserSession session = SessionContext.getSession();
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        messageInfo.setType(messageType.code());
        messageInfo.setRecvId(uid);
        messageInfo.setSendId(session.getUserId());
        messageInfo.setContent(content);
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(uid);
        sendMessage.setSendToSelf(sendToSelf);
        sendMessage.setSendResult(false);
        Integer terminal = getTerminalType(uid, rtcSession);
        sendMessage.setRecvTerminals(Collections.singletonList(terminal));
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
    }

    private void sendActMessage(WebrtcPrivateSession rtcSession, MessageStatus status, String content) {
        // 保存消息
        PrivateMessage msg = new PrivateMessage();
        msg.setSendId(rtcSession.getHost().getId());
        msg.setRecvId(rtcSession.getAcceptor().getId());
        msg.setContent(content);
        msg.setSendTime(new Date());
        msg.setStatus(status.code());
        MessageType type = rtcSession.getMode().equals(WebrtcMode.VIDEO.getValue()) ? MessageType.ACT_RT_VIDEO
            : MessageType.ACT_RT_VOICE;
        msg.setType(type.code());
        privateMessageService.save(msg);
        // 推给发起人
        PrivateMessageVO messageInfo = BeanUtils.copyProperties(msg, PrivateMessageVO.class);
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(rtcSession.getHost());
        sendMessage.setRecvId(rtcSession.getHost().getId());
        sendMessage.setSendToSelf(false);
        sendMessage.setSendResult(true);
        sendMessage.setData(messageInfo);
        imClient.sendPrivateMessage(sendMessage);
        // 推给接听方
        sendMessage.setRecvId(rtcSession.getAcceptor().getId());
        imClient.sendPrivateMessage(sendMessage);
    }

    private String chatTimeText(WebrtcPrivateSession rtcSession) {
        long chatTime = (System.currentTimeMillis() - rtcSession.getChatTimeStamp()) / 1000;
        int min = Math.abs((int)chatTime / 60);
        int sec = Math.abs((int)chatTime % 60);
        String strTime = min < 10 ? "0" : "";
        strTime += min;
        strTime += ":";
        strTime += sec < 10 ? "0" : "";
        strTime += sec;
        return strTime;
    }

    private void detectTimeout(Long chatId, Long userId, Long uid) {
        ScheduledThreadPoolExecutor excutor = ThreadPoolExecutorFactory.getThreadPoolExecutor();
        excutor.schedule(() -> {
            String key = getWebRtcSessionKey(userId, uid);
            WebrtcPrivateSession webrtcSession = (WebrtcPrivateSession)redisTemplate.opsForValue().get(key);
            if (Objects.isNull(webrtcSession) || !chatId.equals(webrtcSession.getChatId())) {
                // 通话已结束
                return;
            }
            // 如果存在了对方的终端类型，说明对方已进入会话
            if (!webrtcSession.getAcceptor().getTerminal().equals(IMTerminalType.UNKNOW.code())) {
                return;
            }
            // 删除会话信息
            removeWebrtcSession(uid, userId);
            // 设置用户空闲状态
            userStateUtils.setFree(uid);
            userStateUtils.setFree(userId);
            // 模拟接听方向发起方推送通话失败信令
            PrivateMessageVO messageInfo = new PrivateMessageVO();
            messageInfo.setType(MessageType.RTC_FAILED.code());
            messageInfo.setRecvId(userId);
            messageInfo.setSendId(uid);
            messageInfo.setContent("对方无应答");
            IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
            sendMessage.setSender(webrtcSession.getAcceptor());
            sendMessage.setRecvId(userId);
            sendMessage.setSendToSelf(true);
            sendMessage.setSendResult(false);
            int terminal = webrtcSession.getHost().getTerminal();
            sendMessage.setRecvTerminals(Collections.singletonList(terminal));
            sendMessage.setData(messageInfo);
            imClient.sendPrivateMessage(sendMessage);
            // 生成消息
            sendActMessage(webrtcSession, MessageStatus.PENDING, "未接通");
            // 停止离线呼叫
            rtcPrivateNotifyService.stop(webrtcSession, "您未接听");
            log.info("通话失败,sid:{},uid:{},reason:{}", userId, uid, "超时");
        }, 30, TimeUnit.SECONDS);
    }

}
