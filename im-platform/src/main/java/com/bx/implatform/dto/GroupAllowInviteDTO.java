package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "允许普通成员邀请好友设置")
public class GroupAllowInviteDTO {

    @NotNull(message = "群id不可为空")
    @Schema(description = "群组id")
    private Long groupId;

    @NotNull(message = "是否允许邀请好友")
    @Schema(description = "是否允许邀请好友")
    private Boolean isAllowInvite;

}
