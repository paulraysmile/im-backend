package com.bx.implatform.config;

import com.bx.implatform.config.props.UnipushProperties;
import com.getui.push.v2.sdk.ApiHelper;
import com.getui.push.v2.sdk.GtApiConfiguration;
import com.getui.push.v2.sdk.api.PushApi;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author: Blue
 * @date: 2024-07-06
 * @version: 1.0
 */
@Data
@Component
@AllArgsConstructor
@ConditionalOnProperty(prefix = "notify", value = "enable", havingValue = "true", matchIfMissing = false)
public class UniPushConfig {

    private final UnipushProperties unipushProperties;

    @Bean
    public GtApiConfiguration uniPushConfiguration(){
        GtApiConfiguration apiConfiguration = new GtApiConfiguration();
        apiConfiguration.setAppId(unipushProperties.getAppId());
        apiConfiguration.setAppKey(unipushProperties.getAppKey());
        apiConfiguration.setMasterSecret(unipushProperties.getMasterSecret());
        return apiConfiguration;
    }

    @Bean
    public PushApi uniPushApi(GtApiConfiguration configuration){
        // 实例化ApiHelper对象，用于创建接口对象
        ApiHelper apiHelper = ApiHelper.build(configuration);
        // 创建对象，建议复用。目前有PushApi、StatisticApi、UserApi
        PushApi pushApi = apiHelper.creatApi(PushApi.class);
        return pushApi;
    }
}
