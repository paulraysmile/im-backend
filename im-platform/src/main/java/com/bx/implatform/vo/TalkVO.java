package com.bx.implatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 动态简要VO
 *
 * @author im-platform
 */
@Data
@Schema(description = "动态简要信息")
public class TalkVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "文件列表JSON")
    private String files;

    @Schema(description = "发布地址")
    private String address;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;
}
