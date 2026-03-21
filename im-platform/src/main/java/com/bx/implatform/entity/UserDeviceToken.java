package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户设备Token实体
 * <p>
 * 用于存储用户设备的 APNs/VoIP 推送 Token，支持离线消息推送
 * </p>
 *
 * @author im-platform
 */
@Data
@TableName("im_user_device_token")
public class UserDeviceToken {

    /** 主键 */
    @TableId
    private Long id;

    /** 用户ID，唯一 */
    private Long userId;

    /** 平台：ios/android */
    private String platform;

    /** 环境：dev/prod */
    private String env;

    /** APNs 推送 Token */
    private String apnsToken;

    /** VoIP（CallKit）Token */
    private String voipToken;

    /** 上报时间 */
    private LocalDateTime reportTime;
}
