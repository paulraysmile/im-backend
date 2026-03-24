package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("im_company")
public class Company {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 企业名称
     */
    private String companyName;

    /**
     * 邀请码
     */
    private String inviteCode;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 删除标识  0：正常   1：已删除
     */
    @TableLogic
    private Boolean deleted;

    /**
     * 创建者
     */
    private Long creator;

    /**
     * 创建时间
     */
    private Date createTime;

}

