package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: Blue
 * @date: 2024-09-07
 * @version: 1.0
 */
@Data
@Schema(description = "系统消息内容详情VO")
public class SystemMessageContentVO {

    @Schema(description = " 消息id")
    private Long id;

    @Schema(description = "内容类型 0:富文本  1:外部链接")
    private Integer contentType;

    @Schema(description = "富文本内容，base64编码")
    private String richText;

    @Schema(description = "externLink")
    private String externLink;

}
