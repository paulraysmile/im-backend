package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "删除消息DTO")
public class MessageDeleteDTO {

    @Schema(description = "会话id,即好友id/群id")
    @NotNull(message = "会话id不可为空")
    private Long chatId;

    @Schema(description = "消息id")
    @NotEmpty(message = "消息id不可为空")
    private List<Long> messageIds;
}
