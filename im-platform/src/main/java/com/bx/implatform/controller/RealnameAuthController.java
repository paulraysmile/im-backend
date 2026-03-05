package com.bx.implatform.controller;

import com.bx.implatform.annotation.RepeatSubmit;
import com.bx.implatform.dto.RealnameAuthDTO;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.RealnameAuthService;
import com.bx.implatform.vo.RealnameAuthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "实名认证")
@RestController
@RequestMapping("/realname/auth")
@RequiredArgsConstructor
public class RealnameAuthController {

    private final RealnameAuthService realnameAuthService;

    @RepeatSubmit
    @PostMapping("/submit")
    @Operation(summary = "发起认证", description = "发起认证")
    public Result submit(@Valid @RequestBody RealnameAuthDTO dto) {
        realnameAuthService.submit(dto);
        return ResultUtils.success();
    }

    @GetMapping("/info")
    @Operation(summary = "查询认证信息", description = "查询认证信息")
    public Result<RealnameAuthVO> info() {
        return ResultUtils.success(realnameAuthService.authInfo());
    }
}
