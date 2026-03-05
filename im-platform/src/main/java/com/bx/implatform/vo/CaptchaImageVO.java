package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;



@Data
@Schema(description = "图片验证码")
public class CaptchaImageVO {


    @Schema(description = "唯一id")
    private String id;

    @Schema(description = "图片base64")
    private String image;
}
