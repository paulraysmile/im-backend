package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态附件文件DTO
 * <p>
 * 用于动态中的图片、视频、音频等附件
 * </p>
 *
 * @author im-platform
 */
@Data
@Schema(description = "附件文件")
public class FileDTO {

    @Schema(description = "文件类型: 1-图片 2-视频 3-音频")
    private Integer fileType;

    @Schema(description = "文件地址")
    private String url;

    @Schema(description = "视频封面图")
    private String coverUrl;
}
