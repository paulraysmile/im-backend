package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bx.imclient.IMClient;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.implatform.annotation.NotifyCheck;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.User;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.enums.WebrtcMode;
import com.bx.implatform.service.RtcPrivateNotifyService;
import com.bx.implatform.service.UserService;
import com.bx.implatform.session.WebrtcPrivateSession;
import com.bx.implatform.thirdparty.UniPushService;
import com.bx.implatform.vo.PrivateMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author: Blue
 * @date: 2024-08-24
 * @version: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RtcPrivateNotifyServiceImpl implements RtcPrivateNotifyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final UniPushService uniPushService;
    private final IMClient imClient;

    @NotifyCheck
    public void setup(WebrtcPrivateSession rtcSession) {
        Long userId = rtcSession.getAcceptor().getId();
        if (imClient.isOnline(userId, IMTerminalType.APP)) {
            return;
        }
        String cid = getCid(userId);
        if (StrUtil.isEmpty(cid)) {
            return;
        }
        User user = getUserInfo(rtcSession.getHost().getId());
        String notifyId = getNotifyId(userId);
        String title = user.getNickName();
        String body = String.format("%s 邀请您进行通话...", getPrefix(rtcSession.getMode()));
        String logo = user.getHeadImageThumb();
        PrivateMessageVO messageInfo = new PrivateMessageVO();
        MessageType messageType = rtcSession.getMode().equals(WebrtcMode.VIDEO.getValue()) ? MessageType.RTC_SETUP_VIDEO
            : MessageType.RTC_SETUP_VOICE;
        messageInfo.setType(messageType.code());
        messageInfo.setSendId(rtcSession.getHost().getId());
        messageInfo.setRecvId(rtcSession.getAcceptor().getId());
        uniPushService.asyncSend(cid, title, body, logo, notifyId, messageInfo);
    }

    @NotifyCheck
    public void stop(WebrtcPrivateSession rtcSession, String tip) {
        Long userId = rtcSession.getAcceptor().getId();
        if (imClient.isOnline(userId, IMTerminalType.APP)) {
            return;
        }
        String cid = getCid(userId);
        if (StrUtil.isEmpty(cid)) {
            return;
        }
        User user = getUserInfo(rtcSession.getHost().getId());
        String notifyId = getNotifyId(userId);
        String title = user.getNickName();
        String body = String.format("%s %s", getPrefix(rtcSession.getMode()), tip);
        String logo = user.getHeadImageThumb();
        uniPushService.asyncSend(cid, title, body, logo, notifyId, null);
    }



    private String getCid(Long userId) {
        String key = StrUtil.join(":", RedisKey.IM_USER_CID, userId);
        return (String)redisTemplate.opsForValue().get(key);
    }

    private String getPrefix(String mode) {
        return mode.equals(WebrtcMode.VIDEO.getValue()) ? "[视频通话]" : "[语音通话]";
    }

    private User getUserInfo(Long userId) {
        // 为提升效率，只取需要的字段
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getId, userId);
        wrapper.select(User::getNickName, User::getHeadImageThumb);
        return userService.getOne(wrapper);
    }

    private String getNotifyId(Long userId) {
        String key = StrUtil.join(":", RedisKey.IM_WEBRTC_PRIVATE_SESSION, userId);
        return Math.abs(key.hashCode()) + "";
    }
}
