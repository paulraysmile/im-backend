package com.bx.implatform.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 注册相关配置。
 *
 * <p>使用显式 getter/setter，避免仅依赖 Lombok 时 IDE 无法解析导致连锁误报。
 */
@Component
@ConfigurationProperties(prefix = "registration")
public class RegistrationProperties {

    /** 注册方式(多种) */
    private List<String> mode;

    /** IP注册限制配置 */
    private IpLimit ipLimit;

    /**
     * 手机/邮箱注册是否必须校验短信或邮件验证码。
     * 默认 {@code false}（不强制）；后续可由 nove_admin 等后台改为 {@code true} 或通过配置中心下发。
     */
    private Boolean requirePhoneEmailCaptcha = false;

    public List<String> getMode() {
        return mode;
    }

    public void setMode(List<String> mode) {
        this.mode = mode;
    }

    public IpLimit getIpLimit() {
        return ipLimit;
    }

    public void setIpLimit(IpLimit ipLimit) {
        this.ipLimit = ipLimit;
    }

    public Boolean getRequirePhoneEmailCaptcha() {
        return requirePhoneEmailCaptcha;
    }

    public void setRequirePhoneEmailCaptcha(Boolean requirePhoneEmailCaptcha) {
        this.requirePhoneEmailCaptcha = requirePhoneEmailCaptcha;
    }

    /** IP 注册上限 */
    public static class IpLimit {

        private Boolean enabled = false;

        private Integer maxCount = 5;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getMaxCount() {
            return maxCount;
        }

        public void setMaxCount(Integer maxCount) {
            this.maxCount = maxCount;
        }
    }
}
