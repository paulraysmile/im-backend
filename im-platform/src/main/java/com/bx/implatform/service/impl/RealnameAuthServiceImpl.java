package com.bx.implatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.dto.RealnameAuthDTO;
import com.bx.implatform.entity.RealnameAuth;
import com.bx.implatform.enums.RealnameAuthStatus;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.RealnameAuthMapper;
import com.bx.implatform.service.RealnameAuthService;
import com.bx.implatform.service.UserService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.RealnameAuthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/**
 * 用户实名认证Service实现类
 *
 * @author Blue
 * @date 2025-12-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealnameAuthServiceImpl extends ServiceImpl<RealnameAuthMapper, RealnameAuth>
    implements RealnameAuthService {

    private final UserService userService;

    @Override
    @Transactional
    public void submit(RealnameAuthDTO dto) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        // 检查是否已有认证记录
        LambdaQueryWrapper<RealnameAuth> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RealnameAuth::getUserId, userId);
        RealnameAuth auth = this.getOne(wrapper);
        if (Objects.isNull(auth)) {
            // 新建认证记录
            auth = new RealnameAuth();
            auth.setUserId(userId);
            auth.setAuthStatus(RealnameAuthStatus.PENDING.getCode());
        } else {
            // 只有待审核或认证失败状态才能重新提交
            if (auth.getAuthStatus().equals(RealnameAuthStatus.APPROVED.getCode())) {
                throw new GlobalException("您已通过实名认证，无需重复提交");
            }
        }
        // 设置认证信息
        auth.setRealName(dto.getRealName());
        auth.setIdCardNumber(dto.getIdCardNumber());
        auth.setIdCardFront(dto.getIdCardFront());
        auth.setIdCardBack(dto.getIdCardBack());
        auth.setAuthStatus(RealnameAuthStatus.PENDING.getCode()); // 重置为待审核状态
        auth.setFailReason(Strings.EMPTY); // 清空失败原因
        auth.setAuditTime(null); // 清空审核时间
        auth.setAuditAdminId(null); // 清空审核管理员
        auth.setAuthTime(new Date());
        this.saveOrUpdate(auth);
        // 更新用户表的认证状态
        userService.updateAuthStatus(RealnameAuthStatus.PENDING);
    }

    @Override
    public RealnameAuthVO authInfo() {
        UserSession session = SessionContext.getSession();
        LambdaQueryWrapper<RealnameAuth> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RealnameAuth::getUserId, session.getUserId());
        RealnameAuth auth = this.getOne(wrapper);
        if (Objects.isNull(auth)) {
            RealnameAuthVO vo =  new RealnameAuthVO();
            vo.setAuthStatus(RealnameAuthStatus.NOT_AUTH.getCode());
            return vo;
        }
        return BeanUtils.copyProperties(auth, RealnameAuthVO.class);
    }
}

