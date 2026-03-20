package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 企业信息（im_company），用于根据 companyId 查企业名称等；不参与分表。
 */
@Data
@TableName("im_company")
public class Company {

    @TableId
    private Long id;
    private String name;
    private String code;
    private String license;
    private String bizScope;
    private String contactPerson;
    private String contactPhone;
    private Boolean deleted;
    private Long creator;
    private java.util.Date createTime;
}
