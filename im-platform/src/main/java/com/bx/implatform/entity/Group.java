package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 群
 *
 * @author blue
 * @since 2022-10-31
 */
@Data
@TableName("im_group")
public class Group {

    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 群名字
     */
    private String name;

    /**
     * 群主id
     */
    private Long ownerId;

    /**
     * 群头像
     */
    private String headImage;

    /**
     * 群头像缩略图
     */
    private String headImageThumb;

    /**
     * 群公告
     */
    private String notice;

    /**
     * 是否开启全体禁言
     */
    private Boolean isAllMuted;

    /**
     * 是否允许普通成员邀请好友
     */
    private Boolean isAllowInvite;

    /**
     * 是否允许普通成员分享名片
     */
    private Boolean isAllowShareCard;

    /**
     * 是否允许普通成员群内互相加好友
     */
    private Boolean isAllowAddOther;

    /**
     * 置顶消息id
     */
    private Long topMessageId;

    /**
     * 是否被封禁
     */
    private Boolean isBanned;

    /**
     * 解除封禁时间
     */
    private Date unbanTime;

    /**
     * 被封禁原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否已删除
     */
    private Boolean dissolve;

}
