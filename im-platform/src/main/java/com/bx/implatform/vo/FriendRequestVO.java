package com.bx.implatform.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 好友添加申请
 * @author Blue
 * @version 1.0
 */
@Data
@Schema(description = "好友添加申请")
public class FriendRequestVO {

    @TableId
    private Long id;

    @Schema(description = "发起方用户ID")
    private Long sendId;

    @Schema(description = "发起方昵称")
    private String sendNickName;

    @Schema(description = "发起方头像")
    private String sendHeadImage;

    @Schema(description = "接收方用户ID")
    private Long recvId;

    @Schema(description = "发起方昵称")
    private String recvNickName;

    @Schema(description = "发起方头像")
    private String recvHeadImage;

    @Schema(description = "申请备注")
    private String remark;

    @Schema(description = "状态  1:待处理 2:同意 3:拒绝")
    private Integer  status;

    @Schema(description = "'申请时间'")
    private Date applyTime;
}
