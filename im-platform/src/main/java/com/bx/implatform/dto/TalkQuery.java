package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;
import lombok.Data;

/**
 * 动态查询请求DTO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态查询")
public class TalkQuery {

    @Schema(description = "上次获取最小动态id，初次查询传空")
    private Long lastMinId;

    @NotNull(message = "预取数量不可为空")
    @Range(min = 1, max = 100, message = "预取数量范围为1-100")
    @Schema(description = "预取数量（取值范围: 1-100）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer prefetchSize;
}
