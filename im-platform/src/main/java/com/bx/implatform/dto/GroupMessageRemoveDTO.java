package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 移除群聊消息DTO
 *
 * @author im-platform
 */
@Data
@Schema(description = "移除群聊消息")
public class GroupMessageRemoveDTO {

    @NotEmpty(message = "消息id列表不可为空")
    @Schema(description = "消息id列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> messageIds;

    @NotNull(message = "群聊id不可为空")
    @Schema(description = "群聊id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long groupId;
}
