package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户状态
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "用户当前状态VO")
public class UserStateVO {

    @Schema(description = "状态类型，枚举: UserStateType")
    private Integer type;

    @Schema(description = "数据,不同状态存放不同数据量")
    private Object data;
}
