package com.bx.implatform.dto;

import com.bx.implatform.contant.PatternText;
import com.bx.implatform.enums.RegisterMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "重置密码DTO")
public class ResetPwdDTO {

    @Pattern(regexp = PatternText.INVITE_CODE, message = "邀请码为6位数字或字母")
    @NotEmpty(message = "邀请码不可为空")
    @Schema(description = "企业邀请码，与注册时一致")
    private String inviteCode;

    @Schema(description = "验证方式, phone:手机注册,email: 邮箱注册")
    private String mode = RegisterMode.USER_NAME.getCode() ;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Length(min = 5, max = 20, message = "密码长度必须在5-20个字符之间")
    @NotEmpty(message = "用户密码不可为空")
    @Schema(description = "用户密码")
    private String password;

    @Schema(description = "验证码")
    private String code;

}
