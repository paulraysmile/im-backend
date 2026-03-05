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
@ConfigurationProperties(prefix = "notify")
public class NotifyProperties {

    private Boolean enable;

    private Boolean debug;

    private Integer maxSize;

    private Integer activeDays;

    private String appName;

    private String packageName;

    private UnipushProperties unipush;

    private ManufacturerProperties manufacturer;


}
