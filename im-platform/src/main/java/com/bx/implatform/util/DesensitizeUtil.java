package com.bx.implatform.util;

import cn.hutool.core.util.StrUtil;

/**
 * 数据脱敏工具类
 *
 * @author blue
 */
public class DesensitizeUtil {

    /**
     * 手机号脱敏
     * 规则：显示前3位和后4位，中间用****代替
     * 例如：138****5678
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String desensitizePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        // 手机号长度通常是11位
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        // 如果不是11位，则只显示前3位和后2位
        if (phone.length() > 5) {
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
        }
        // 如果长度太短，则全部用*代替
        return "****";
    }

    /**
     * 邮箱脱敏
     * 规则：显示@前面的前2位和后2位，中间用****代替，@后面的域名完整显示
     * 例如：ab****cd@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String desensitizeEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex == -1) {
            // 如果没有@符号，则按普通字符串处理
            if (email.length() > 4) {
                return email.substring(0, 2) + "****" + email.substring(email.length() - 2);
            }
            return "****";
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() > 4) {
            return localPart.substring(0, 2) + "****" + localPart.substring(localPart.length() - 2) + domain;
        } else if (localPart.length() > 2) {
            return localPart.substring(0, 1) + "****" + localPart.substring(localPart.length() - 1) + domain;
        } else {
            return "****" + domain;
        }
    }
}
