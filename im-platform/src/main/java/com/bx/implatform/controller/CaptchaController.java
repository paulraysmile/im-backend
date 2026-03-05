package com.bx.implatform.controller;

import com.bx.implatform.dto.SendMailCodeDTO;
import com.bx.implatform.dto.SendSmsCodeDTO;
import com.bx.implatform.enums.CaptchaType;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.CaptchaService;
import com.bx.implatform.vo.CaptchaImageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 验证码
 */
@Tag(name = "验证码相关接口")
@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "生成图形验证码", description = "生成图形验证码")
    @PostMapping("/img/code")
    public Result<CaptchaImageVO> generateImageCode() {
        return ResultUtils.success(captchaService.generateImageCode());
    }

    @Operation(summary = "发送短信验证码", description = "发送短信验证码")
    @PostMapping("/sms/code")
    public Result sendSmsCode(@RequestBody SendSmsCodeDTO dto) {
        captchaService.sendSmsCode(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "发送邮箱验证码", description = "发送邮箱验证码")
    @PostMapping("/mail/code")
    public Result sendMailCode(@RequestBody SendMailCodeDTO dto) {
        captchaService.sendMailCode(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "校验短信验证码", description = "校验短信验证码")
    @GetMapping("/sms/vertify")
    public Result<Boolean> vertifySmsCode(@RequestParam String id, @RequestParam String code) {
        return ResultUtils.success(captchaService.vertify(CaptchaType.SMS, id, code));
    }

    @Operation(summary = "校验邮件验证码", description = "校验邮件验证码")
    @GetMapping("/mail/vertify")
    public Result<Boolean> vertifyMailCode(@RequestParam String id, @RequestParam String code) {
        return ResultUtils.success(captchaService.vertify(CaptchaType.MAIL, id, code));
    }

    @Operation(summary = "校验图形验证码", description = "校验图形验证码")
    @GetMapping("/img/vertify")
    public Result<Boolean> vertifyImageCode(@RequestParam String id, @RequestParam String code) {
        return ResultUtils.success(captchaService.vertify(CaptchaType.IMAGE, id, code));
    }

}
