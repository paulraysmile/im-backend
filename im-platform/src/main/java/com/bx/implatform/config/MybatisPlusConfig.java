package com.bx.implatform.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 配置：企业维度动态表名。
 * 对 im_private_message、im_group、im_group_member、im_group_message 按当前用户的 companyId 路由到
 * 表名_c_{companyId}，实现企业分表；companyId 为空时使用原表名（兼容历史及未绑定企业用户）。
 */
@Configuration
public class MybatisPlusConfig {

    private static final Set<String> COMPANY_TABLES = Set.of(
        "im_private_message",
        "im_group",
        "im_group_member",
        "im_group_message"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DynamicTableNameInnerInterceptor dynamicTableName = new DynamicTableNameInnerInterceptor();
        dynamicTableName.setTableNameHandler((sql, tableName) -> {
            if (!COMPANY_TABLES.contains(tableName)) {
                return tableName;
            }
            UserSession session = SessionContext.getSession();
            if (session == null || session.getCompanyId() == null) {
                return tableName;
            }
            return tableName + "_c_" + session.getCompanyId();
        });
        interceptor.addInnerInterceptor(dynamicTableName);
        return interceptor;
    }
}
