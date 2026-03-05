package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 手机绑定
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "手机绑定")
public class BindPhoneDTO {

    @NotEmpty(message = "手机号码不可为空")
    @Schema(description = "手机号码")
    private String phone;

    @NotEmpty(message = "验证不可为空")
    @Schema(description = "验证码")
    private String code;
    
}
