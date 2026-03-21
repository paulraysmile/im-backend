package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 动态评论请求DTO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态评论")
public class TalkCommentDTO {

    @NotNull(message = "动态id不可为空")
    @Schema(description = "动态id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long talkId;

    @NotBlank(message = "评论内容不可为空")
    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "回复的评论id")
    private Long replyCommentId;

    @NotNull(message = "评论类型不可为空")
    @Schema(description = "评论类型: 0-文字 1-图片 5-语音台词", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer type;
}
