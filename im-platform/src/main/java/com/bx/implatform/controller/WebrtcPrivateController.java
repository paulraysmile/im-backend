package com.bx.implatform.controller;

import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.WebrtcPrivateService;
import com.bx.implatform.vo.WebrtcPrivateInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "单人通话")
@RestController
@RequestMapping("/webrtc/private")
@RequiredArgsConstructor
public class WebrtcPrivateController {

    private final WebrtcPrivateService webrtcPrivateService;

    @Operation(summary = "呼叫视频通话")
    @PostMapping("/setup")
    public Result setup(@RequestParam Long uid, @RequestParam(defaultValue = "video") String mode) {
        webrtcPrivateService.setup(uid, mode);
        return ResultUtils.success();
    }

    @Operation(summary = "接受视频通话")
    @PostMapping("/accept")
    public Result accept(@RequestParam Long uid) {
        webrtcPrivateService.accept(uid);
        return ResultUtils.success();
    }

    @Operation(summary = "拒绝视频通话")
    @PostMapping("/reject")
    public Result reject(@RequestParam Long uid) {
        webrtcPrivateService.reject(uid);
        return ResultUtils.success();
    }

    @Operation(summary = "取消呼叫")
    @PostMapping("/cancel")
    public Result cancel(@RequestParam Long uid) {
        webrtcPrivateService.cancel(uid);
        return ResultUtils.success();
    }

    @Operation(summary = "呼叫失败")
    @PostMapping("/failed")
    public Result failed(@RequestParam Long uid, @RequestParam String reason) {
        webrtcPrivateService.failed(uid, reason);
        return ResultUtils.success();
    }

    @Operation(summary = "挂断")
    @PostMapping("/handup")
    public Result handup(@RequestParam Long uid) {
        webrtcPrivateService.handup(uid);
        return ResultUtils.success();
    }

    @Operation(summary = "推送offer信息")
    @PostMapping("/offer")
    public Result offer(@RequestParam Long uid, @RequestBody String offer) {
        webrtcPrivateService.offer(uid, offer);
        return ResultUtils.success();
    }

    @Operation(summary = "推送answer信息")
    @PostMapping("/answer")
    public Result answer(@RequestParam Long uid, @RequestBody String answer) {
        webrtcPrivateService.answer(uid, answer);
        return ResultUtils.success();
    }

    @PostMapping("/candidate")
    @Operation(summary = "同步candidate")
    public Result candidate(@RequestParam Long uid, @RequestBody String candidate) {
        webrtcPrivateService.candidate(uid, candidate);
        return ResultUtils.success();
    }

    @Operation(summary = "心跳")
    @PostMapping("/heartbeat")
    public Result heartbeat(@RequestParam Long uid) {
        webrtcPrivateService.heartbeat(uid);
        return ResultUtils.success();
    }

    @Operation(summary = "获取通话信息")
    @GetMapping("/info")
    public Result<WebrtcPrivateInfoVO> info(@RequestParam("uid") Long uid) {
        return ResultUtils.success(webrtcPrivateService.info(uid));
    }
}
