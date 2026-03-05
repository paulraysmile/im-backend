package com.bx.implatform.controller;

import com.bx.implatform.annotation.RepeatSubmit;
import com.bx.implatform.dto.UserComplaintDTO;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.UserComplainService;
import com.bx.implatform.vo.UserComplaintVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Tag(name = "用户投诉")
@RestController
@RequestMapping("/complaint")
@RequiredArgsConstructor
public class UserComplaintController {

    private final UserComplainService userComplainService;

    @RepeatSubmit(interval = 10, timeUnit = TimeUnit.MINUTES)
    @PostMapping("/initiate")
    @Operation(summary = "发起投诉", description = "发起投诉")
    public Result initiate(@Valid @RequestBody UserComplaintDTO dto) {
        userComplainService.initiate(dto);
        return ResultUtils.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询投诉列表", description = "查询投诉列表,一次最多拉取10条")
    public Result<List<UserComplaintVO>> findPage(@RequestParam(defaultValue = "-1") Long maxPageId,
        @RequestParam(defaultValue = "-1") Integer status) {
        return ResultUtils.success(userComplainService.findPage(maxPageId, status));
    }

    @GetMapping("/info")
    @Operation(summary = "查询投诉详细信息", description = "查询投诉详细信息")
    public Result<UserComplaintVO> findPage(@NotNull(message = "投诉id不可为空") @RequestParam Long id) {
        return ResultUtils.success(userComplainService.findById(id));
    }

}
