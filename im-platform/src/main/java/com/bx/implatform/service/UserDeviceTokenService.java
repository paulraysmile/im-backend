package com.bx.implatform.service;

import com.bx.implatform.dto.DeviceTokenDTO;

import java.util.Map;
import java.util.Set;

/**
 * 用户设备Token服务
 * <p>
 * 用于上报、存储用户设备的推送 Token（APNs/VoIP）
 * </p>
 *
 * @author im-platform
 */
public interface UserDeviceTokenService {

    /**
     * 上报用户设备Token并落库
     * <p>
     * 根据 type 区分：voip 存入 voip_token，其他存入 apns_token
     * </p>
     *
     * @param dto 设备Token信息
     */
    void reportDeviceToken(DeviceTokenDTO dto);

    /**
     * 批量查询 iOS 用户的 APNs Token
     * <p>
     * 仅返回 platform 为 ios 且 apns_token 非空的记录
     * </p>
     *
     * @param userIds 用户ID集合
     * @return userId -> apnsToken 映射
     */
    Map<Long, String> getApnsTokensByUserIds(Set<Long> userIds);
}
