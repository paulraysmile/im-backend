package com.bx.implatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FriendRequestStatus {

    PENDING(1, "待处理"),
    APPROVED(2, "同意"),
    REJECTED(3, "拒绝"),
    RECALL(4, "撤回"),
    EXPIRED(5, "过期");

    private final Integer code;

    private final String desc;
}
