package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 新增动态请求DTO
 * <p>
 * 用于发布朋友圈动态的请求参数
 * </p>
 *
 * @author im-platform
 */
@Data
@Schema(description = "新增动态")
public class TalkAddDTO {

    @Schema(description = "内容")
    private String content;

    @NotNull(message = "可见范围不可为空")
    @Schema(description = "可见范围: 1-私密 2-好友可见 9-公开", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer visibleScope;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "经度")
    private Double longitude;

    @Valid
    @Schema(description = "文件列表（图片、视频、音频）")
    private List<FileDTO> files;
}
