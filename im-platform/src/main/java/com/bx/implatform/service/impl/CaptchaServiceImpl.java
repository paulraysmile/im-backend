package com.bx.implatform.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.bx.implatform.config.props.MailProperties;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.dto.SendMailCodeDTO;
import com.bx.implatform.dto.SendSmsCodeDTO;
import com.bx.implatform.enums.CaptchaType;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.service.CaptchaService;
import com.bx.implatform.thirdparty.sms.SmsAPI;
import com.bx.implatform.vo.CaptchaImageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码相关
 *
 * @author Blue
 * @version 1.0
 * @date 2025-03-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final SmsAPI smsApi;

    private final MailAccount mailAccount;

    private final MailProperties mailProps;
    /**
     * 验证码失效时长
     */
    private final int expiredTime = 5;

    @Override
    public CaptchaImageVO generateImageCode() {
        String id = UUID.randomUUID().toString();
        //定义图形验证码的长和宽
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 50, 4, 10);

        // 记录到redis
        String key = StrUtil.join(":", RedisKey.IM_CAPTCHA_IMAGE, id);
        redisTemplate.opsForValue().set(key, captcha.getCode(), expiredTime, TimeUnit.MINUTES);
        //图形验证码写出，可以写出到文件，也可以写出到流
        CaptchaImageVO vo = new CaptchaImageVO();
        vo.setId(id);
        vo.setImage(captcha.getImageBase64());
        return vo;
    }

    @Override
    public void sendSmsCode(SendSmsCodeDTO dto) {
        // 校验图形验证码
        if (!vertify(CaptchaType.IMAGE, dto.getId(), dto.getCode())) {
            throw new GlobalException("图形验证码未通过");
        }
        // 生成验证码
        String code = generateCode();
        // 发送短信
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("code", code);
        smsApi.send(dto.getPhone(), paramMap);
        // 验证码记录到redis
        String key = StrUtil.join(":", RedisKey.IM_CAPTCHA_SMS, dto.getPhone());
        redisTemplate.opsForValue().set(key, code, expiredTime, TimeUnit.MINUTES);
    }

    @Override
    public void sendMailCode(SendMailCodeDTO dto) {
        // 生成验证码
        String code = generateCode();
        // 发送邮件
        String subject = mailProps.getSubject();
        String content = mailProps.getContent().replace("${code}", code);
        MailUtil.send(mailAccount, dto.getEmail(), subject, content, false);
        // 验证码记录到redis
        String key = StrUtil.join(":", RedisKey.IM_CAPTCHA_MAIL, dto.getEmail());
        redisTemplate.opsForValue().set(key, code, expiredTime, TimeUnit.MINUTES);
    }

    @Override
    public Boolean vertify(CaptchaType type, String id, String code) {
        String key = StrUtil.join(":", RedisKey.IM_CAPTCHA, type.getCode(), id);
        String captcha = (String)redisTemplate.opsForValue().get(key);
        if (StrUtil.isEmpty(captcha)) {
            return false;
        }
        return code.equalsIgnoreCase(captcha);
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

}
