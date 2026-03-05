package com.bx.implatform.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import com.bx.implatform.config.props.MailProperties;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Blue
 * @version 1.0
 */
@Configuration
@AllArgsConstructor
public class MailConfig {

    private final MailProperties props;

    @Bean
    MailAccount mailAccount() {
        MailAccount mailAccount = new MailAccount();
        mailAccount.setHost(props.getHost());
        mailAccount.setPort(props.getPort());
        mailAccount.setSslEnable(props.getSsl());
        mailAccount.setFrom(props.getFrom());
        if(StrUtil.isNotEmpty(props.getName())){
            // 指定发件人名称
            mailAccount.setFrom(StrUtil.format("{} <{}>",  props.getName(), props.getFrom()));
        }
        mailAccount.setPass(props.getPass());
        mailAccount.setUser(props.getFrom());
        return mailAccount;
    }

}
