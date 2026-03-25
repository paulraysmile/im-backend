package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移除全部群聊消息DTO
 */
@Data
@Schema(description = "移除全部群聊消息")
public class GroupMessageRemoveAllDTO {

    @Schema(description = "消息id（会话中最后一条消息id）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "消息id不可为空")
    private Long messageId;

    @Schema(description = "群聊id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群聊id不可为空")
    private Long groupId;
}
