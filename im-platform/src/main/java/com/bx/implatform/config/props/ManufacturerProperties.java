package com.bx.implatform.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: Blue
 * @date: 2024-09-18
 * @version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "notify.manufacturer")
public class ManufacturerProperties {

    private String xmChannelId;

    private String hwCategory;

    private String opCategory;

    private String vvCategory;

}
