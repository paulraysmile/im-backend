package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SmsPlatformType {

    /**
     * 阿里云短信
     */
    ALIYUN("aliyun", "阿里云"),
    /**
     * 未知
     */
    UNKONW("unkone", "未知");

    private final String code;

    private final String desc;

    public static SmsPlatformType fromCode(String code) {
        for (SmsPlatformType typeEnum : values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return SmsPlatformType.UNKONW;
    }
}

