package com.bx.implatform.thirdparty.sms;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.bx.implatform.config.props.SmsProperties;
import com.bx.implatform.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 阿里云短信api
 * @author Blue
 * @version 1.0
 * @date 2025-03-21
 */
@Slf4j
public class AliyunSmsAPI implements SmsAPI{
    private final SmsProperties props;
    private final Client client;


    public AliyunSmsAPI(SmsProperties props){
        this.props = props;
        Config config = new Config();
        config.setAccessKeyId(this.props.getAccessKey());
        config.setAccessKeySecret(this.props.getSecretKey());
        config.endpoint = "dysmsapi.aliyuncs.com";
        try {
            this.client = new Client(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(String phone, Map<String,String> paramMap){
        SendSmsRequest request = new SendSmsRequest();
        request.setSignName(this.props.getSignName());
        request.setTemplateCode(this.props.getTemplateId());
        request.setPhoneNumbers(phone);;
        if (MapUtil.isNotEmpty(paramMap)) {
            request.setTemplateParam(JSON.toJSONString(paramMap));
        }
        try {
            SendSmsResponse response = client.sendSms(request);
            // 发送失败
            if (!"OK".equalsIgnoreCase(response.getBody().getCode())) {
                throw new GlobalException(response.getBody().getMessage());
            }
        } catch (Exception e) {
            log.error("短信验证码发送失败",e);
            throw new GlobalException("发送失败");
        }
    }
}
