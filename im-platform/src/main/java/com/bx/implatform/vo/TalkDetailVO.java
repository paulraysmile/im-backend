package com.bx.implatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 动态详情VO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态详情")
public class TalkDetailVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "文件列表JSON")
    private String files;

    @Schema(description = "可见范围: 1-私密 2-好友可见 9-公开")
    private Integer visibleScope;

    @Schema(description = "发布地址")
    private String address;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "是否自己的")
    private Boolean isOwner;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "动态点赞列表")
    private List<TalkStarVO> talkStarVOS;

    @Schema(description = "动态评论列表")
    private List<TalkCommentVO> talkCommentVOS;

    @Schema(description = "用户展示信息Map，key为用户id")
    private Map<Long, UserShowVO> userShowMap;
}
