package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
    /**
     * 正常
     */
    NORMAL(0),
    /**
     * 注销
     */
    UN_REG(1);

    private final Integer value;

}
