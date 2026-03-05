package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RegisterMode {

    /**
     * 用户名
     */
    USER_NAME("username", "用户名注册"),
    /**
     * 用户名
     */
    PHONE("phone", "手机注册"),
    /**
     * 用户名
     */
    EMAIL("email", "邮箱注册"),

    /**
     * 未知
     */
    UNKONW("unkone", "未知");
    private final String code;

    private final String desc;

    public static RegisterMode fromCode(String code) {
        for (RegisterMode typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return RegisterMode.UNKONW;
    }
}

