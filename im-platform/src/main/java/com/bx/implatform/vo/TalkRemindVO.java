package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态未读提醒VO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态未读提醒")
public class TalkRemindVO {

    @Schema(description = "未读点赞和评论数量")
    private Long notifyCount;

    @Schema(description = "动态id")
    private Long talkId;

    @Schema(description = "头像")
    private String avatar;
}
