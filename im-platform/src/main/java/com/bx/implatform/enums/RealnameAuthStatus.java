package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 实名认证状态枚举
 *
 * @author Blue
 * @date 2025-12-01
 */
@Getter
@AllArgsConstructor
public enum RealnameAuthStatus {

    /**
     * 未认证
     */
    NOT_AUTH(0, "未认证"),
    /**
     * 待审核
     */
    PENDING(1, "待审核"),

    /**
     * 已认证
     */
    APPROVED(2, "已认证"),

    /**
     * 认证失败
     */
    FAILED(3, "认证失败");

    private final Integer code;

    private final String desc;

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 枚举值
     */
    public static RealnameAuthStatus fromCode(Integer code) {
        for (RealnameAuthStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

