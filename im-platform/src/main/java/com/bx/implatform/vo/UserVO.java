package com.bx.implatform.vo;

import com.bx.implatform.contant.PatternText;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.Date;

@Data
@Schema(description = "用户信息VO")
public class UserVO {

    @NotNull(message = "用户id不能为空")
    @Schema(description = "id")
    private Long id;

    @NotEmpty(message = "用户名不能为空")
    @Length(max = 20, message = "用户名不能大于20字符")
    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "邮箱")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Pattern( regexp= PatternText.NICK_NAME,message = "昵称包含不合法字符")
    @NotEmpty(message = "用户昵称不能为空")
    @Length(max = 32, message = "昵称不能大于32字符")
    @Schema(description = "用户昵称")
    private String nickName;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "用户类型 1:普通用户 2:审核账户")
    private Integer type;

    @Length(max = 128, message = "个性签名不能大于128个字符")
    @Schema(description = "个性签名")
    private String signature;

    @Schema(description = "头像")
    private String headImage;

    @Schema(description = "头像缩略图")
    private String headImageThumb;

    @Schema(description = "归属企业名称")
    private String companyName;

    @Schema(description = "账号是否被封禁")
    private Boolean isBanned;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "解除封禁时间")
    private Date unbanTime;

    @Schema(description = "被封禁原因")
    private String reason;

    @Schema(description = "是否开启好友验证审核")
    private Boolean isManualApprove;

    @Schema(description = "是否在黑名单中")
    private Boolean isInBlacklist;

    @Schema(description = "是否开启新消息语音提醒")
    private Boolean isAudioTip;

    @Schema(description = "状态 0:正常 1:已注销")
    private Integer status;

    @Schema(description = "实名认证状态 0:未认证 1:审核中 2:已认证 3:认证失败")
    private Integer authStatus;

    @Schema(description = "是否在线")
    private Boolean online;

}
