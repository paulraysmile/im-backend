package com.bx.implatform.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱配置
 * @author Blue
 * @version 1.0
 * @date 2025-03-21
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String host;

    private int port;

    private Boolean ssl;

    private String name;

    private String from;

    private String pass;

    private String subject;

    private String content;

}
