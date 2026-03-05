package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "允许普通成员分享名片设置")
public class GroupAllowShareCardDTO {

    @NotNull(message = "群id不可为空")
    @Schema(description = "群组id")
    private Long groupId;

    @NotNull(message = "是否允许分享名片")
    @Schema(description = "是否允许分享名片")
    private Boolean isAllowShareCard;

}
