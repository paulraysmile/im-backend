package com.bx.implatform.controller;

import com.bx.implatform.annotation.RepeatSubmit;
import com.bx.implatform.dto.FriendRequestApplyDTO;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.FriendRequestService;
import com.bx.implatform.vo.FriendRequestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "好友审核")
@RestController
@RequestMapping("/friend/request")
@RequiredArgsConstructor
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @GetMapping("/list")
    @Operation(summary = "好友申请列表", description = "好友申请列表")
    public Result<List<FriendRequestVO>> loadNearlyList() {
        return ResultUtils.success(friendRequestService.loadPendingList());
    }

    @RepeatSubmit
    @PostMapping("/apply")
    @Operation(summary = "添加好友申请", description = "添加好友申请")
    public Result<FriendRequestVO> apply(@Valid @RequestBody FriendRequestApplyDTO dto) {
        return ResultUtils.success(friendRequestService.apply(dto));
    }

    @RepeatSubmit
    @PostMapping("/approve")
    @Operation(summary = "同意好友申请", description = "同意好友申请")
    public Result approve(@RequestParam Long id) {
        friendRequestService.approve(id);
        return ResultUtils.success();
    }

    @RepeatSubmit
    @PostMapping("/reject")
    @Operation(summary = "拒绝好友申请", description = "拒绝好友申请")
    public Result reject(@RequestParam Long id) {
        friendRequestService.reject(id);
        return ResultUtils.success();
    }

    @RepeatSubmit
    @PostMapping("/recall")
    @Operation(summary = "撤回好友申请", description = "撤回好友申请")
    public Result recall(@RequestParam Long id) {
        friendRequestService.recall(id);
        return ResultUtils.success();
    }

}
