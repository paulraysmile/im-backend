package com.bx.implatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.entity.InviteCode;
import com.bx.implatform.mapper.InviteCodeMapper;
import com.bx.implatform.service.InviteCodeService;
import org.springframework.stereotype.Service;

/**
 * 邀请码校验：仅查 im_invite_code 表，不走企业分表。
 */
@Service
public class InviteCodeServiceImpl extends ServiceImpl<InviteCodeMapper, InviteCode> implements InviteCodeService {

    @Override
    public InviteCode getValidByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<InviteCode> q = Wrappers.lambdaQuery();
        q.eq(InviteCode::getCode, code.trim());
        q.eq(InviteCode::getEnabled, true);
        q.last("limit 1");
        return getOne(q);
    }

    @Override
    public InviteCode getByCodeForLogin(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<InviteCode> q = Wrappers.lambdaQuery();
        q.eq(InviteCode::getCode, code.trim());
        q.last("limit 1");
        return getOne(q);
    }
}
