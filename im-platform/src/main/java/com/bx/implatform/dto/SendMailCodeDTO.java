package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 发送邮箱验证码
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "发送邮箱验证码DTO")
public class SendMailCodeDTO {

    @NotEmpty(message = "邮箱不可为空")
    @Schema(description = "邮箱")
    private String email;


}
