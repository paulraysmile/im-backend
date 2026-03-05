package com.bx.implatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "好友信息VO")
public class FriendVO {

    @NotNull(message = "好友id不可为空")
    @Schema(description = "好友id")
    private Long id;

    @NotNull(message = "好友昵称不可为空")
    @Schema(description = "好友昵称")
    private String nickName;

    @Schema(description = "群内显示名称")
    private String showNickName;

    @Schema(description = "群内昵称备注")
    private String remarkNickName;

    @Schema(description = "好友头像")
    private String headImage;

    @Schema(description = "好友所属公司名称")
    private String companyName;

    @Schema(description = "是否开启免打扰")
    private Boolean isDnd;

    @Schema(description = "是否置顶会话")
    private Boolean isTop;

    @Schema(description = "是否已删除")
    private Boolean deleted;

    @Schema(description = "是否在线")
    private Boolean online;

    @Schema(description = "网页端是否在线")
    private Boolean onlineWeb;

    @Schema(description = "APP端是否在线")
    private Boolean onlineApp;
}
