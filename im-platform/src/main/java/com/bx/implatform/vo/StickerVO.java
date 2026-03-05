package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "表情包VO")
public class StickerVO {

    @Schema(description = "表情ID")
    private Long id;

    @Schema(description = "所属专辑ID")
    private Long albumId;

    @Schema(description = "表情名称")
    private String name;

    @Schema(description = "表情图片URL")
    private String imageUrl;

    @Schema(description = "缩略图URL")
    private String thumbUrl;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;
}