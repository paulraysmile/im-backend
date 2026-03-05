package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码登录状态VO")
public class QrLoginStatusVO {

    @Schema(description = "登录状态: WAITING-等待扫码, SCANNED-已扫码, CONFIRMED-已确认, EXPIRED-已过期")
    private String status;

    @Schema(description = "登录信息(仅在CONFIRMED状态时返回)")
    private LoginVO loginInfo;

    @Schema(description = "状态描述")
    private String message;

}
