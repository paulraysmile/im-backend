package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户黑名单
 * @author: Blue
 * @date: 2024-09-22
 * @version: 1.0
 */
@Data
@TableName("im_user_blacklist")
public class UserBlacklist {

    @TableId
    private Long id;

    /**
     * 拉黑用户id
     */
    private Long fromUserId;

    /**
     * 被拉黑用户id
     */
    private Long toUserId;

    /**
     * 拉黑时间
     */
    private Date createTime;
}
