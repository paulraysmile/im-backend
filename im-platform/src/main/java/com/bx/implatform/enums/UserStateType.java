package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStateType {
    /**
     * 空闲
     */
    FREE(0, "空闲"),
    /**
     * 单人通话中
     */
    RTC_PRIVATE(1, "单人通话中"),
    /**
     * 多人通话中
     */
    RTC_GROUP(2, "多人通话中");

    private final Integer code;

    private final String desc;
}
