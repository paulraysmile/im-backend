package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 发送短信验证码
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "发送短信验证码DTO")
public class SendSmsCodeDTO {

    @NotEmpty(message = "手机号码不可为空")
    @Schema(description = "手机号码")
    private String phone;

    @NotEmpty(message = "图形验证码id不可为空")
    @Schema(description = "图形验证码id")
    private String id;

    @NotEmpty(message = "图形验证码不可为空")
    @Schema(description = "图形验证码")
    private String  code;

}
