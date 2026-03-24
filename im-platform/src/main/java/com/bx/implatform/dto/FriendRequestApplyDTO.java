package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 好友添加申请
 *
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "好友添加申请")
public class FriendRequestApplyDTO {

    @NotNull(message = "好友id不可为空")
    @Schema(description = "好友用户id")
    private Long friendId;

    @Schema(description = "申请备注")
    private String remark;

}
