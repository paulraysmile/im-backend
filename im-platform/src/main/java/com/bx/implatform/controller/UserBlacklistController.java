package com.bx.implatform.controller;

import com.bx.implatform.annotation.RepeatSubmit;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.UserBlacklistService;
import com.bx.implatform.session.SessionContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户黑名单")
@RestController
@RequestMapping("/blacklist")
@RequiredArgsConstructor
public class UserBlacklistController {

    private final UserBlacklistService userBlacklistService;

    @RepeatSubmit
    @PostMapping("/add")
    @Operation(summary = "加入黑名单", description = "加入黑名单")
    public Result add(@RequestParam Long userId) {
        userBlacklistService.add(SessionContext.getSession().getUserId(), userId);
        return ResultUtils.success();
    }

    @DeleteMapping("/remove")
    @Operation(summary = "移除黑名单", description = "移除黑名单")
    public Result remove(@RequestParam Long userId) {
        userBlacklistService.remove(SessionContext.getSession().getUserId(), userId);
        return ResultUtils.success();
    }
}
