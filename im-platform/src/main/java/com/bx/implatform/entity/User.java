package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author blue
 * @since 2022-10-01
 */
@Data
@TableName("im_user")
public class User {

    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 归属企业id
     */
    private Long companyId;

    /**
     * 归属企业名称
     */
    private String companyName;

    /**
     * 组id
     */
    private Long groupId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String headImage;

    /**
     * 用户头像缩略图
     */
    private String headImageThumb;

    /**
     * 密码(明文)
     */
    private String password;

    /**
     * 性别 0:男 1::女
     */
    private Integer sex;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 账号是否被封禁
     */
    private Boolean isBanned;

    /**
     * 解除封禁时间
     */
    private Date unbanTime;

    /**
     * 账号被封禁原因
     */
    private String reason;

    /**
     * 是否手动验证好友请求
     */
    private Boolean isManualApprove;

    /**
     * 客户端id,用于uni-push推送
     */
    private String cid;

    /**
     *  最后登录时间
     */
    private Date lastLoginTime;

    /**
     *  最后登录ip
     */
    private String lastLoginIp;

    /**
     * 状态 0:正常 1:已注销
     */
    private Integer status;

    /**
     * 新消息语音提醒 bit-0:web端 bit-1:app端
     */
    private Integer audioTip;

    /**
     * 实名认证状态，0-未认证 1-待审核 2-已认证 3-认证失败
     */
    private Integer authStatus;

    /**
     * 创建时间(注册时间)
     */
    private Date createTime;

    /**
     *  用户类型 1:普通用户 2:公开测试账户 3:审核专用账户
     */
    private Integer type;

}
