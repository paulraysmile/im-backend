package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户设备Token DTO
 * <p>
 * 用于上报设备推送Token（cid），用于离线消息推送
 * </p>
 *
 * @author im-platform
 */
@Data
@Schema(description = "用户设备Token")
public class DeviceTokenDTO {

    @NotBlank(message = "token不可为空")
    @Schema(description = "设备推送Token(cid)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "平台")
    private String platform;

    @Schema(description = "环境")
    private String env;
}
