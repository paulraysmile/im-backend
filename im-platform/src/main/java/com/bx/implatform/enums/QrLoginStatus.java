package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QrLoginStatus {

    WAITING("等待扫码"),
    SCANNED("已扫码"),
    CONFIRMED("已确认"),
    EXPIRED("已过期");

    private final String description;

}
