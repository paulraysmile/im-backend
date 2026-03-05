package com.bx.implatform.config;

import com.bx.implatform.config.props.SmsProperties;
import com.bx.implatform.enums.SmsPlatformType;
import com.bx.implatform.thirdparty.sms.AliyunSmsAPI;
import com.bx.implatform.thirdparty.sms.SmsAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化短信配置
 * @author Blue
 * @version 1.0
 * @date 2025-03-21
 */
@Configuration
@RequiredArgsConstructor
public class SmsConfig {

    private final SmsProperties props;

    @Bean
    SmsAPI smsAPI(){
        SmsPlatformType platform = SmsPlatformType.fromCode(props.getPlatform());
        return switch (platform) {
            case ALIYUN -> new AliyunSmsAPI(props);
            default ->  null;
        };
    }
}
