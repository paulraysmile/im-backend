package com.bx.implatform.vo;

import com.bx.implatform.config.props.AppProperties;
import com.bx.implatform.config.props.RegistrationProperties;
import com.bx.implatform.config.props.WebrtcProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: blue
 * @date: 2024-06-10
 * @version: 1.0
 */
@Data
@Schema(description = "系统配置VO")
public class SystemConfigVO {

    @Schema(description = "app是否正在审核中")
    private Boolean appInReview = false;

    @Schema(description = "webrtc配置")
    private WebrtcProperties webrtc;

    @Schema(description = "注册相关配置")
    private RegistrationProperties registration;

    @Schema(description = "app信息")
    private AppProperties app;

}
