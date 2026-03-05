package com.bx.implatform.vo;

import com.bx.imcommon.model.IMUserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: Blue
 * @date: 2024-08-28
 * @version: 1.0
 */
@Data
@Schema(description = "群通话信息VO")
public class WebrtcPrivateInfoVO {

    @Schema(description = "是否在通话中")
    private Boolean isChating;

    @Schema(description = "通话发起者")
    private IMUserInfo host;

    @Schema(description = "通话被邀请者")
    private IMUserInfo acceptor;

    @Schema(description = "通话模式")
    private String mode;



}
