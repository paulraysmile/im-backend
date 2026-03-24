package com.bx.implatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.entity.Company;
import com.bx.implatform.mapper.CompanyMapper;
import com.bx.implatform.service.CompanyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CompanyServiceImpl extends ServiceImpl<CompanyMapper, Company> implements CompanyService {

    @Override
    public Long selectIdByInviteCode(String inviteCode) {
        Company company = this.lambdaQuery()
                .select(Company::getId)
                .eq(Company::getInviteCode, inviteCode)
                .one();
        return company != null ? company.getId() : null;
    }
}
