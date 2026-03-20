package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 邀请码：6 位数字或字母，用于注册时绑定企业，实现企业维度分表隔离。
 */
@Data
@TableName("im_invite_code")
public class InviteCode {

    @TableId
    private Long id;
    /** 邀请码，6 位数字或字母，唯一 */
    private String code;
    /** 关联企业 id，对应 im_company.id */
    private Long companyId;
    /** 是否启用 0:否 1:是 */
    private Boolean enabled;
    private Date createTime;
    private String remark;
}
