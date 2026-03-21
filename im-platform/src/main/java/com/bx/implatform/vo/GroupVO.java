package com.bx.implatform.vo;

import com.bx.implatform.contant.PatternText;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Date;

@Data
@Schema(description = "群信息VO")
public class GroupVO {

    @Schema(description = "群id")
    private Long id;

    @Pattern( regexp= PatternText.NICK_NAME,message = "群名包含不合法字符")
    @Length(max = 32, message = "群名称长度不能大于32")
    @NotEmpty(message = "群名称不可为空")
    @Schema(description = "群名称")
    private String name;

    @Schema(description = "群主id")
    private Long ownerId;

    @Schema(description = "头像")
    private String headImage;

    @Schema(description = "头像缩略图")
    private String headImageThumb;

    @Length(max = 1024, message = "群聊显示长度不能大于1024")
    @Schema(description = "群公告")
    private String notice;

    @Pattern( regexp= PatternText.NICK_NAME,message = "显示昵称包含不合法字符")
    @Length(max = 20, message = "显示昵称长度不能大于20")
    @Schema(description = "用户在群显示昵称")
    private String remarkNickName;

    @Schema(description = "群内显示名称")
    private String showNickName;

    @Schema(description = "群名显示名称")
    private String showGroupName;

    @Pattern( regexp= PatternText.NICK_NAME,message = "群名备注包含不合法字符")
    @Length(max = 32, message = "群名备注长度不能大于32")
    @Schema(description = "群名备注")
    private String remarkGroupName;

    @Schema(description = "是否开启全体禁言")
    private Boolean isAllMuted;

    @Schema(description = "是否允许普通成员邀请好友")
    private Boolean isAllowInvite;

    @Schema(description = "是否允许普通成员分享名片")
    private Boolean isAllowShareCard;

    @Schema(description = "是否允许普通成员群内互相加好友")
    private Boolean isAllowAddOther;

    @Schema(description = "是否已解散")
    private Boolean dissolve;

    @Schema(description = "是否已退出")
    private Boolean quit;

    @Schema(description = "是否被禁言")
    private Boolean isMuted;

    @Schema(description = "群聊是否被封禁")
    private Boolean isBanned;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "解除封禁时间")
    private Date unbanTime;

    @Schema(description = "被封禁原因")
    private String reason;

    @Schema(description = "是否开启免打扰")
    private Boolean isDnd;

    @Schema(description = "是否置顶会话")
    private Boolean isTop;

    @Schema(description = "置顶消息")
    private GroupMessageVO topMessage;

    @Schema(description = "群通话信息")
    private WebrtcGroupInfoVO rtcInfo;

}
