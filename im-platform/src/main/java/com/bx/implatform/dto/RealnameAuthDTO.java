package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 用户实名认证VO
 *
 * @author Blue
 * @date 2025-12-01
 */
@Data
@Schema(description = "用户实名认证DTO")
public class RealnameAuthDTO {

    @NotEmpty(message = "真实姓名不可为空")
    @Schema(description = "真实姓名")
    private String realName;

    @NotEmpty(message = "证件号码不可为空")
    @Schema(description = "证件号码")
    private String idCardNumber;

    @NotEmpty(message = "请上传身份证正面照片")
    @Schema(description = "身份证正面照片URL")
    private String idCardFront;

    @NotEmpty(message = "请上传身份证反面照片")
    @Schema(description = "身份证反面照片URL")
    private String idCardBack;

}

