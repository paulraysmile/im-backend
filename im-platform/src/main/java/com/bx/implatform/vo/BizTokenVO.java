package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "业务tokenVO")
public class BizTokenVO {

    @Schema(description = "token类型 1:群名片分享 2:群二维码")
    private Integer type;

    @Schema(description = "token")
    private String token;

    @Schema(description = "过期时间(时间戳)")
    private Long expiredIn;
}
