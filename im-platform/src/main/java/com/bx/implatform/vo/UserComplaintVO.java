package com.bx.implatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 用户投诉对象
 *
 * @author Blue
 * @date 2025-06-23
 */
@Data
@Schema(description = "用户投诉VO")
public class UserComplaintVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "投诉对象类型 1:用户 2:群聊")
    private Integer targetType;

    @Schema(description = "投诉对象id")
    private Long targetId;

    @Schema(description = "投诉对象名称")
    private String targetName;

    @Schema(description = "投诉原因类型 1:对我造成骚扰 2:疑似诈骗行为 3:传播不良内容  99:其他")
    private Integer type;

    @Schema(description = "图片列表,最多9张")
    private List<UploadImageVO> images;

    @Schema(description = "投诉内容")
    private String content;

    @Schema(description = "状态 1:未处理 2:已处理")
    private Integer status;

    @Schema(description = "处理结果类型 1:核实并处理 2:未核实 3:投诉内容不涉及违规 4:其他")
    private Integer resolvedType;

    @Schema(description = "处理结果摘要")
    private String resolvedSummary;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间")
    private String resolvedTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建(投诉)时间")
    private Date createTime;

}
