package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 系统消息推送任务
 *
 * @author Blue
 * @since 1.0.0 2024-09-04
 */

@Data
@TableName("im_sm_push_task")
public class SmPushTask {
    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 系统消息id
     */
    private Long messageId;

    /**
     * 发送序列号
     */
    private Long seqNo;

    /**
     * 推送时间
     */
    private Date sendTime;

    /**
     * 状态 1:待发送 2:发送中 3:已发送 4:已取消
     */
    private Integer status;

    /**
     * 是否推送所有用户
     */
    private Boolean sendToAll;

    /**
     * 接收用户id,逗号分隔,sendToAll为false时有效
     */
    private String recvIds;

    @TableLogic
    private Integer deleted;

}