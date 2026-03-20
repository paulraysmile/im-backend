package com.bx.implatform.dto;

import com.bx.implatform.contant.PatternText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "用户登录DTO")
public class LoginDTO {

    @Pattern(regexp = PatternText.INVITE_CODE, message = "邀请码为6位数字或字母")
    @NotEmpty(message = "邀请码不可为空")
    @Schema(description = "企业邀请码，与注册时一致，用于定位租户")
    private String inviteCode;

    @Max(value = 2, message = "登录终端类型取值范围:0,2")
    @Min(value = 0, message = "登录终端类型取值范围:0,2")
    @NotNull(message = "登录终端类型不可为空")
    @Schema(description = "登录终端 0:web 1:app 2:pc")
    private Integer terminal;

    @NotEmpty(message = "登陆名不可为空")
    @Schema(description = "登陆名: 用户名/手机号/邮箱")
    private String userName;

    @NotEmpty(message = "用户密码不可为空")
    @Schema(description = "用户密码")
    private String password;

}
