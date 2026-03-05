package com.bx.implatform.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信配置
 * @author Blue
 * @version 1.0
 * @date 2025-03-21
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private String platform;

    private String accessKey;

    private String secretKey;

    private String templateId;

    private String signName;

}
