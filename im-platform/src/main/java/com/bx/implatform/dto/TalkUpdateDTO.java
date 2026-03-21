package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新动态请求DTO
 * <p>
 * 用于修改动态的可见范围
 * </p>
 *
 * @author im-platform
 */
@Data
@Schema(description = "修改动态")
public class TalkUpdateDTO {

    @NotNull(message = "动态id不可为空")
    @Schema(description = "动态id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull(message = "可见范围不可为空")
    @Schema(description = "可见范围: 1-私密 2-好友可见 9-公开", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer visibleScope;
}
