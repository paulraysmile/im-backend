package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户组
 */
@Data
@TableName("im_user_group")
public class UserGroup {

    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 组名
     */
    private Long groupName;

    /**
     * 权限
     *      friend_add: 允许加好友(发起好友申请，如果不给这个权限的分组下账号将不能发起好友请求);
     *      group_create: 允许建群，只有有建群权限的分组下账号才能建群;
     *      friend_fast_add: 允许直接加好友，次功能可直接添加对方为好友，不需要对方同意(比如导师，客服，证券经理)次功能强大，不要随便移动账号到这个分组下;
     *      user_login_app: 允许app登陆，关闭次功能将不能登陆App;
     *      user_login_web: 允许登陆web，关闭这个功能的分组下账号将不能登陆web端;
     *      ip_whitelist: ip白名单，次功能配合系统设置-前端ip白名单功能使用，分组启用ip白名单功能的时候，会先检索系统配置-前端ip白名单里面有没有这个ip，有ip的才能登陆分组下面的账号，次功能适用于重要的账号比如人设号，导师号，客服和证券经理等;
     */
    private String permission;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
