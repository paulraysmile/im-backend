package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.RealnameAuthDTO;
import com.bx.implatform.entity.RealnameAuth;
import com.bx.implatform.vo.RealnameAuthVO;

/**
 * 用户实名认证Service
 *
 * @author Blue
 * @date 2025-12-01
 */
public interface RealnameAuthService extends IService<RealnameAuth> {

    /**
     * 提交实名认证申请
     *
     * @param dto 实名认证信息
     */
    void submit(RealnameAuthDTO dto);

    /**
     * 查询当前用户的实名认证信息
     *
     * @return 实名认证信息
     */
    RealnameAuthVO authInfo();




}

