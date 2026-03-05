package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "扫码登录VO")
public class QrLoginVO {

    @Schema(description = "二维码唯一标识")
    private String qrCode;

    @Schema(description = "二维码图片Base64编码")
    private String qrImage;

    @Schema(description = "二维码过期时间(秒)")
    private Integer expiresIn;

}
