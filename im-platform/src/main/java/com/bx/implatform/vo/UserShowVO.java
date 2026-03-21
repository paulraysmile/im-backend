package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户展示信息VO
 *
 * @author im-platform
 */
@Data
@Schema(description = "用户展示信息")
public class UserShowVO {

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "用户昵称")
    private String nickName;

    @Schema(description = "头像缩略图")
    private String headImageThumb;
}
