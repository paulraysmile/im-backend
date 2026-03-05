package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "群二维码DTO")
public class GroupQrcodeDTO {

    @Schema(description = "用户id")
    Long userId;

    @Schema(description = "群id")
    Long groupId;

}
