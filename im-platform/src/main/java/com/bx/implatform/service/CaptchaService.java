package com.bx.implatform.service;

import com.bx.implatform.dto.SendMailCodeDTO;
import com.bx.implatform.dto.SendSmsCodeDTO;
import com.bx.implatform.enums.CaptchaType;
import com.bx.implatform.vo.CaptchaImageVO;

public interface CaptchaService {

    /**
     * 生成图形验证码
     */
    CaptchaImageVO generateImageCode();

    /**
     * 发送短信验证码
     * @param dto dto
     */
    void sendSmsCode(SendSmsCodeDTO dto);

    /**
     * 发送邮箱验证码
     * @param dto dto
     */
    void sendMailCode(SendMailCodeDTO dto);

    /**
     * 验证码校验
     * @param type 类型:图片、短信、邮箱
     * @param id 唯一标识:uuid、手机号码、邮箱
     * @param code 验证码
     * @return
     */
    Boolean vertify(CaptchaType type, String id, String code);
}
