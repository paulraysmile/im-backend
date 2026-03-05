package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 邮箱绑定
 */
@Data
@Schema(description = "邮箱绑定")
public class BindEmailDTO {

    @NotEmpty(message = "邮箱不可为空")
    @Schema(description = "邮箱")
    private String email;

    @NotEmpty(message = "验证不可为空")
    @Schema(description = "验证码")
    private String code;

}
