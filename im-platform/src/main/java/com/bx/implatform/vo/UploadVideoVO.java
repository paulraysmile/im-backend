package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: Blue
 * @date: 2024-11-17
 * @version: 1.0
 */
@Data
@Schema(description = "视频上传VO")
public class UploadVideoVO {

    @Schema(description = "视频url")
    private String videoUrl;

    @Schema(description = "封面url")
    private String coverUrl;

    @Schema(description = "图片宽度")
    private int width;

    @Schema(description = "图片高度")
    private int height;

}
