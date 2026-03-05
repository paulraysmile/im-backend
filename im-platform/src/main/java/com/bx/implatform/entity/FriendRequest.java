package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 好友请求
 * @author Blue
 * @version 1.0
 */
@Data
@TableName("im_friend_request")
public class FriendRequest {

    @TableId
    private Long id;

    /**
     * 发起方用户ID
     */
    private Long sendId;

    /**
     * 发起方昵称,冗余字段
     */
    private String sendNickName;


    /**
     * 发起方昵称,冗余字段
     */
    private String sendHeadImage;

    /**
     * 接收方用户ID
     */
    private Long recvId;

    /**
     * 接收方昵称,冗余字段
     */
    private String recvNickName;

    /**
     * 接收方昵称,冗余字段
     */
    private String recvHeadImage;

    /**
     * 申请备注
     */
    private String remark;


    /**
     * 状态  1:待处理 2:同意 3:拒绝
     */
    private Integer  status;

    /**
     * '申请时间'
     */
    private Date applyTime;

}
