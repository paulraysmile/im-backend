package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.dto.DeviceTokenDTO;
import com.bx.implatform.entity.UserDeviceToken;
import com.bx.implatform.mapper.UserDeviceTokenMapper;
import com.bx.implatform.service.UserDeviceTokenService;
import com.bx.implatform.session.SessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户设备Token服务实现
 * <p>
 * user_id 唯一，采用 upsert：存在则更新，不存在则插入
 * </p>
 *
 * @author im-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceTokenServiceImpl extends ServiceImpl<UserDeviceTokenMapper, UserDeviceToken>
        implements UserDeviceTokenService {

    private static final String TYPE_VOIP = "voip";

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void reportDeviceToken(DeviceTokenDTO dto) {
        Long userId = SessionContext.getSession().getUserId();
        boolean isVoip = TYPE_VOIP.equalsIgnoreCase(StrUtil.nullToEmpty(dto.getType()).trim());

        LambdaQueryWrapper<UserDeviceToken> query = Wrappers.lambdaQuery();
        query.eq(UserDeviceToken::getUserId, userId);
        UserDeviceToken existing = getOne(query);

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            LambdaUpdateWrapper<UserDeviceToken> update = Wrappers.lambdaUpdate();
            update.eq(UserDeviceToken::getUserId, userId);
            update.set(UserDeviceToken::getReportTime, now);
            if (StrUtil.isNotBlank(dto.getPlatform())) {
                update.set(UserDeviceToken::getPlatform, trimLen(dto.getPlatform(), 16));
            }
            if (StrUtil.isNotBlank(dto.getEnv())) {
                update.set(UserDeviceToken::getEnv, trimLen(dto.getEnv(), 16));
            }
            if (isVoip) {
                update.set(UserDeviceToken::getVoipToken, trimLen(dto.getToken(), 128));
            } else {
                update.set(UserDeviceToken::getApnsToken, trimLen(dto.getToken(), 128));
            }
            update(update);
            log.debug("更新用户设备Token, userId:{}, type:{}", userId, isVoip ? "voip" : "apns");
        } else {
            UserDeviceToken entity = new UserDeviceToken();
            entity.setUserId(userId);
            entity.setReportTime(now);
            entity.setPlatform(trimLen(dto.getPlatform(), 16));
            entity.setEnv(trimLen(dto.getEnv(), 16));
            if (isVoip) {
                entity.setVoipToken(trimLen(dto.getToken(), 128));
            } else {
                entity.setApnsToken(trimLen(dto.getToken(), 128));
            }
            save(entity);
            log.debug("新增用户设备Token, userId:{}, type:{}", userId, isVoip ? "voip" : "apns");
        }
    }

    @Override
    public Map<Long, String> getApnsTokensByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<UserDeviceToken> query = Wrappers.lambdaQuery();
        query.in(UserDeviceToken::getUserId, userIds);
        query.isNotNull(UserDeviceToken::getApnsToken);
        query.ne(UserDeviceToken::getApnsToken, "");
        return list(query).stream()
            .filter(e -> StrUtil.isNotBlank(e.getApnsToken()))
            .collect(Collectors.toMap(UserDeviceToken::getUserId, UserDeviceToken::getApnsToken, (a, b) -> a));
    }

    private static String trimLen(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
