package com.bx.implatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 动态点赞VO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态点赞")
public class TalkStarVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "动态id")
    private Long talkId;

    @Schema(description = "用户id")
    private Long userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "是否自己的")
    private Boolean isOwner;
}
