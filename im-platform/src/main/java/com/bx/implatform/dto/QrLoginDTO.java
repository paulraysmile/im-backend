package com.bx.implatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "扫码登录DTO")
public class QrLoginDTO {

    @NotEmpty(message = "二维码标识不可为空")
    @Schema(description = "二维码唯一标识")
    private String qrCode;

}
