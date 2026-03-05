package com.bx.implatform.thirdparty.sms;

import java.util.Map;

public interface SmsAPI {

    /**
     * 发送短信
     * @param phone 手机号码
     * @param paramMap 模版参数
     */
    void send(String phone, Map<String,String> paramMap);
}
