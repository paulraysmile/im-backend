package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务token类型
 *
 * @author Blue
 * @date 2025-12-31
 */
@Getter
@AllArgsConstructor
public enum BizTokenType {

    /**
     * 群名片
     */
    GROUP_CARD(1, "群名片"),

    /**
     * 群二维码
     */
    GROUP_QRCODE(2, "群二维码");

    private final Integer code;

    private final String desc;

    /**
     * 根据code获取枚举
     *
     * @param code 类型码
     * @return 枚举值
     */
    public static BizTokenType fromCode(Integer code) {
        for (BizTokenType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

