package com.bx.implatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 用户实名认证VO
 *
 * @author Blue
 * @date 2025-12-01
 */
@Data
@Schema(description = "用户实名认证VO")
public class RealnameAuthVO {

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "证件号码")
    private String idCardNumber;

    @Schema(description = "身份证正面照片URL")
    private String idCardFront;

    @Schema(description = "身份证反面照片URL")
    private String idCardBack;

    @Schema(description = "认证状态: 1-待审核，2-已认证，3-认证失败")
    private Integer authStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审核时间")
    private Date authTime;

    @Schema(description = "审核未通过原因")
    private String failReason;


}

