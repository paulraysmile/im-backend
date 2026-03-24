package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 动态评论实体
 *
 * @author im-platform
 */
@Data
@TableName("im_talk_comment")
public class TalkComment {

    @TableId
    private Long id;

    /**
     * 动态id
     */
    private Long talkId;

    /**
     * 评论用户id
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 回复的评论id
     */
    private Long replyCommentId;

    /**
     * 回复用户id
     */
    private Long replyUserId;

    /**
     * 评论类型: 0-文字 1-图片 5-语音台词
     */
    private Integer type;

    /**
     * 创建时间
     */
    private Date createdTime;
}
