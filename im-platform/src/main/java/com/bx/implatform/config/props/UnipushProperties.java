package com.bx.implatform.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Blue
 * @date: 2024-08-21
 * @version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "notify.uni-push")
public class UnipushProperties {

    /** 是否启用个推，仅使用 APNs 时设为 false 避免启动时校验失败 */
    private Boolean enabled = false;

    private String appId;

    private String appKey;

    private String masterSecret;
}
