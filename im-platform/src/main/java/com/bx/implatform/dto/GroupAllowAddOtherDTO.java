package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 允许普通成员群内互相加好友设置
 *
 * @author im-platform
 */
@Data
@Schema(description = "允许普通成员群内互相加好友设置")
public class GroupAllowAddOtherDTO {

    @NotNull(message = "群id不可为空")
    @Schema(description = "群组id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long groupId;

    @NotNull(message = "是否允许不可为空")
    @Schema(description = "是否允许普通成员群内互相加好友", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isAllowAddOther;
}
