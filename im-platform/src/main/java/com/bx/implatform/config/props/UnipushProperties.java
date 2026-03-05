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

    private String appId;

    private String appKey;

    private String   masterSecret;
}
