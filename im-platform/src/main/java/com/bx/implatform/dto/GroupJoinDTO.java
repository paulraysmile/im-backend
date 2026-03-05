package com.bx.implatform.dto;

import com.bx.implatform.vo.BizTokenVO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "加入群聊请求")
public class GroupJoinDTO {

    @NotNull(message = "群id不可为空")
    @Schema(description = "群id")
    private Long groupId;

    @NotNull(message = "token不可为空")
    @Schema(description = "入群token（通过二维码或名片分享获取）")
    private BizTokenVO token;
}

