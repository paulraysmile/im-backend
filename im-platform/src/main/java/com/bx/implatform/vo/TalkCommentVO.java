package com.bx.implatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 动态评论VO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态评论")
public class TalkCommentVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "动态id")
    private Long talkId;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "回复的评论id")
    private Long replyCommentId;

    @Schema(description = "回复用户id")
    private Long replyUserId;

    @Schema(description = "评论类型: 0-文字 1-图片 5-语音台词")
    private Integer type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "是否自己的")
    private Boolean isOwner;
}
