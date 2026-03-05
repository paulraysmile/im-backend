package com.bx.implatform.dto;

import com.bx.implatform.vo.UploadImageVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 用户发起投诉
 *
 * @author Blue
 * @date 2025-06-23
 */
@Data
@Schema(description = "用户发起投诉DTO")
public class UserComplaintDTO {

    @NotNull(message = "投诉对象类型不能为空")
    @Schema(description = "投诉对象类型 1:用户 2:群聊")
    private Integer targetType;

    @NotNull(message = "投诉对象id不能为空")
    @Schema(description = "投诉对象id")
    private Long targetId;

    @NotNull(message = "投诉对象名称不能为空")
    @Schema(description = "投诉对象名称")
    private String targetName;

    @NotNull(message = "投诉原因不能为空")
    @Schema(description = "投诉原因 1:对我造成骚扰 2:疑似诈骗行为 3:传播不良内容 99:其他")
    private Integer type;

    @Schema(description = "图片列表,最多9张")
    private List<UploadImageVO> images;

    @NotEmpty(message = "投诉内容不能为空")
    @Length(max = 512, message = "投诉内容不能大于512字符")
    @Schema(description = "投诉内容")
    private String content;

}
