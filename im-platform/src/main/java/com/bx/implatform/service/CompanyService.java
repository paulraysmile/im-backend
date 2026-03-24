package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.Company;

public interface CompanyService extends IService<Company> {

    Long selectIdByInviteCode(String inviteCode);

}
