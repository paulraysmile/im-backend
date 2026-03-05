package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "表情包专辑VO")
public class StickerAlbumVO {

    @Schema(description = "专辑ID")
    private Long id;

    @Schema(description = "专辑名称")
    private String name;

    @Schema(description = "专辑logo")
    private String logoUrl;
}