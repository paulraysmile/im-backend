package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CaptchaType {

    /**
     * 图形验证码
     */
    IMAGE("img", "图形验证码"),
    /**
     *  短信验证码
     */
    SMS("sms", "短信验证码"),

    /**
     *  邮件证码
     */
    MAIL("mail", "邮件证码");

    private final String code;

    private final String desc;

}

