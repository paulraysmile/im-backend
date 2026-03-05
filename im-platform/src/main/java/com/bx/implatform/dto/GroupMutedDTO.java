package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Blue
 * @version 1.0
 * @date 2025-02-23
 */
@Data
@Schema(description = "群聊全员禁言")
public class GroupMutedDTO {

    @NotNull(message = "群id不可为空")
    @Schema(description = "群组id")
    private Long id;

    @NotNull(message = "禁言状态不可为空")
    @Schema(description = "禁言状态: 开启/关闭")
    private Boolean isMuted;
}
