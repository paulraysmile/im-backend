package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "群信息VO")
public class GroupInfoVO {

    @Schema(description = "群id")
    private Long groupId;

    @Schema(description = "群名称")
    private String groupNname;

    @Schema(description = "头像缩略图")
    private String headImageThumb;

}
