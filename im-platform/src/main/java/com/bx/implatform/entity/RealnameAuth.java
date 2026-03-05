package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户实名认证
 *
 * @author Blue
 * @date 2025-12-01
 */
@Data
@TableName("im_realname_auth")
public class RealnameAuth {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件号码
     */
    private String idCardNumber;

    /**
     * 身份证正面照片URL
     */
    private String idCardFront;

    /**
     * 身份证反面照片URL
     */
    private String idCardBack;

    /**
     * 认证状态：1-待审核，2-已认证，3-认证失败
     */
    private Integer authStatus;

    /**
     * 审核管理员ID
     */
    private Long auditAdminId;

    /**
     * 审核时间
     */
    private Date auditTime;

    /**
     * 审核未通过原因
     */
    private String failReason;

    /**
     * 发起认证时间
     */
    private Date authTime;

    /**
     * 创建时间
     */
    private Date createTime;

}

