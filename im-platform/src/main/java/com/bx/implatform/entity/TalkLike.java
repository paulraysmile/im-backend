package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 动态点赞实体
 *
 * @author im-platform
 */
@Data
@TableName("im_talk_star")
public class TalkLike {

    @TableId
    private Long id;

    /** 动态id */
    private Long talkId;

    /** 用户id */
    private Long userId;

    /** 创建时间 */
    private Date createdTime;
}
