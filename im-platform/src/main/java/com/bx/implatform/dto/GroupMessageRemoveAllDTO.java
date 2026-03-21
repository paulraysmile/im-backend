package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移除全部群聊消息DTO
 *
 * @author im-platform
 */
@Data
@Schema(description = "移除全部群聊消息")
public class GroupMessageRemoveAllDTO {

    @NotNull(message = "消息id不可为空")
    @Schema(description = "消息id（会话中最后一条消息id）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long messageId;

    @NotNull(message = "群聊id不可为空")
    @Schema(description = "群聊id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long groupId;
}
