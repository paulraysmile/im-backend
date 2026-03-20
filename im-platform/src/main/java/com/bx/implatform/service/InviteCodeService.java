package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.InviteCode;

/**
 * 邀请码服务：校验 6 位邀请码并返回绑定企业信息，供注册时写入 user.companyId/companyName。
 */
public interface InviteCodeService extends IService<InviteCode> {

    /**
     * 根据邀请码获取有效记录（启用状态），不存在或未启用返回 null。
     */
    InviteCode getValidByCode(String code);

    /**
     * 登录时解析企业：仅按邀请码匹配，不校验 enabled，避免停用邀请码后老用户无法登录。
     */
    InviteCode getByCodeForLogin(String code);
}
