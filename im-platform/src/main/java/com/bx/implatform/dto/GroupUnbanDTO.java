package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "群组解锁")
public class GroupUnbanDTO {

    @Schema(description = "群组id")
    private Long id;

    @Schema(description = "公司id")
    private Long companyId;

}
