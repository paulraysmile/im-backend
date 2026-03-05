package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户表情包收藏VO
 *
 * @author Blue
 * @date 2025-12-31
 */
@Data
@Schema(description = "用户自定义表情包VO")
public class StickerCustomVO {

    @Schema(description = "表情id")
    private Long id;

    @Schema(description = "专辑id")
    private Long albumId;

    @Schema(description = "表情id")
    private Long stickerId;

    @Schema(description = "表情名称")
    private String name;

    @Schema(description = "表情图片url")
    private String imageUrl;

    @Schema(description = "缩略图url")
    private String thumbUrl;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

}

