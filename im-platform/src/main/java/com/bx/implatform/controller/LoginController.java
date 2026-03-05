package com.bx.implatform.controller;

import com.bx.implatform.dto.*;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.QrLoginService;
import com.bx.implatform.service.UserService;
import com.bx.implatform.vo.LoginVO;
import com.bx.implatform.vo.QrLoginStatusVO;
import com.bx.implatform.vo.QrLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "注册登录")
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final QrLoginService qrLoginService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return ResultUtils.success(vo);
    }

    @PutMapping("/refreshToken")
    @Operation(summary = "刷新token", description = "用refreshtoken换取新的token")
    public Result refreshToken(@RequestHeader("refreshToken") String refreshToken) {
        LoginVO vo = userService.refreshToken(refreshToken);
        return ResultUtils.success(vo);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册")
    public Result register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return ResultUtils.success();
    }

    @DeleteMapping("/unregister")
    @Operation(summary = "用户注销", description = "用户注册")
    public Result unregister() {
        userService.unregister();
        return ResultUtils.success();
    }

    @PutMapping("/modifyPwd")
    @Operation(summary = "修改密码", description = "修改用户密码")
    public Result modifyPassword(@Valid @RequestBody ModifyPwdDTO dto) {
        userService.modifyPassword(dto);
        return ResultUtils.success();
    }

    @PutMapping("/resetPwd")
    @Operation(summary = "重置密码", description = "重置用户密码")
    public Result resetPassword(@Valid @RequestBody ResetPwdDTO dto) {
        userService.resetPassword(dto);
        return ResultUtils.success();
    }

    @PostMapping("/qrLogin/generate")
    @Operation(summary = "生成扫码登录二维码", description = "生成扫码登录二维码")
    public Result<QrLoginVO> generateQrCode() {
        QrLoginVO vo = qrLoginService.generateQrCode();
        return ResultUtils.success(vo);
    }

    @GetMapping("/qrLogin/status/{qrCode}")
    @Operation(summary = "查询扫码登录状态", description = "查询扫码登录状态")
    public Result<QrLoginStatusVO> getQrLoginStatus(@PathVariable String qrCode) {
        QrLoginStatusVO vo = qrLoginService.getLoginStatus(qrCode);
        return ResultUtils.success(vo);
    }

    @PostMapping("/qrLogin/scan")
    @Operation(summary = "扫描二维码", description = "移动端扫描二维码")
    public Result scanQrCode(@Valid @RequestBody QrLoginDTO dto) {
        qrLoginService.scanQrCode(dto.getQrCode());
        return ResultUtils.success();
    }

    @PostMapping("/qrLogin/confirm")
    @Operation(summary = "确认扫码登录", description = "确认扫码登录")
    public Result<LoginVO> confirmQrLogin(@Valid @RequestBody QrLoginDTO dto) {
        LoginVO vo = qrLoginService.confirmQrLogin(dto.getQrCode());
        return ResultUtils.success(vo);
    }

    @DeleteMapping("/qrLogin/cancel/{qrCode}")
    @Operation(summary = "取消扫码登录", description = "取消扫码登录")
    public Result cancelQrLogin(@PathVariable String qrCode) {
        qrLoginService.cancelQrLogin(qrCode);
        return ResultUtils.success();
    }

}
