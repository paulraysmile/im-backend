package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 移除群聊消息DTO
 */
@Data
@Schema(description = "移除群聊消息")
public class GroupMessageRemoveDTO {

    @Schema(description = "消息id列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "消息id列表不可为空")
    @Size(max = 50, message = "一次最多只能移除50条消息哦")
    private List<Long> messageIds;

    @Schema(description = "群聊id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群聊id不可为空")
    private Long groupId;

}
