package com.bx.implatform.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * APNs 推送配置
 * <p>
 * 用于苹果设备离线消息直连推送，需在 Apple Developer 创建 .p8 密钥
 * </p>
 *
 * @author im-platform
 */
@Data
@Component
@ConfigurationProperties(prefix = "notify.apns")
public class ApnsProperties {

    /** 是否启用 APNs 直连推送 */
    private Boolean enabled = false;

    /** 密钥 ID（.p8 文件名中 AuthKey_XXX 的 XXX 部分） */
    private String keyId;

    /** 开发团队 ID */
    private String teamId;

    /** App Bundle ID */
    private String bundleId;

    /** .p8 密钥文件路径 */
    private String keyPath;

    /** 是否生产环境，false 为 development（沙箱） */
    private Boolean production = true;
}
