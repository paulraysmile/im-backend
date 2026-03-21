package com.bx.implatform.controller;

import com.bx.implatform.annotation.RepeatSubmit;
import com.bx.implatform.dto.TalkAddDTO;
import com.bx.implatform.dto.TalkCommentDTO;
import com.bx.implatform.dto.TalkQuery;
import com.bx.implatform.dto.TalkUpdateDTO;
import com.bx.implatform.result.Result;
import com.bx.implatform.vo.TalkDetailListVO;
import com.bx.implatform.vo.TalkDetailVO;
import com.bx.implatform.vo.TalkRemindVO;
import com.bx.implatform.vo.TalkVO;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.TalkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 朋友圈动态控制器
 *
 * @author im-platform
 */
@Tag(name = "朋友圈动态")
@RestController
@RequestMapping("/talk")
@RequiredArgsConstructor
public class TalkController {

    private final TalkService talkService;

    @GetMapping
    @Operation(summary = "查询动态", description = "分页查询好友动态列表")
    public Result<TalkDetailListVO> query(
        @RequestParam(required = false) Integer prefetchSize,
        @RequestParam(required = false, name = "prefechSize") Integer prefechSizeAlias,
        @RequestParam(required = false) String lastMinId) {
        TalkQuery query = buildTalkQuery(prefetchSize, prefechSizeAlias, lastMinId);
        return ResultUtils.success(talkService.query(query));
    }

    @GetMapping("/detail/{talkId}")
    @Operation(summary = "查询动态详情", description = "根据动态id查询单条动态详情，含点赞和评论")
    public Result<TalkDetailVO> getDetail(@NotNull(message = "动态id不可为空") @PathVariable Long talkId) {
        return ResultUtils.success(talkService.getDetail(talkId));
    }

    @GetMapping("/friend")
    @Operation(summary = "查询好友动态", description = "分页查询好友发布的动态")
    public Result<List<TalkVO>> queryFriend(
        @RequestParam(required = false) Integer prefetchSize,
        @RequestParam(required = false, name = "prefechSize") Integer prefechSizeAlias,
        @RequestParam(required = false) String lastMinId) {
        TalkQuery query = buildTalkQuery(prefetchSize, prefechSizeAlias, lastMinId);
        return ResultUtils.success(talkService.queryFriend(query));
    }

    @GetMapping("/self")
    @Operation(summary = "查询自己动态", description = "分页查询当前用户自己发布的动态")
    public Result<List<TalkVO>> querySelf(
        @RequestParam(required = false) Integer prefetchSize,
        @RequestParam(required = false, name = "prefechSize") Integer prefechSizeAlias,
        @RequestParam(required = false) String lastMinId) {
        TalkQuery query = buildTalkQuery(prefetchSize, prefechSizeAlias, lastMinId);
        return ResultUtils.success(talkService.querySelf(query));
    }

    /** 兼容 prefechSize 拼写错误及 lastMinId=nil */
    private TalkQuery buildTalkQuery(Integer prefetchSize, Integer prefechSizeAlias, String lastMinId) {
        Integer size = prefetchSize != null ? prefetchSize : prefechSizeAlias;
        if (size == null) {
            throw new GlobalException("预取数量不可为空");
        }
        if (size < 1 || size > 100) {
            throw new GlobalException("预取数量范围为1-100");
        }
        TalkQuery query = new TalkQuery();
        query.setPrefetchSize(size);
        if (StrUtil.isNotBlank(lastMinId) && !"nil".equalsIgnoreCase(lastMinId) && !"null".equalsIgnoreCase(lastMinId)) {
            try {
                query.setLastMinId(Long.parseLong(lastMinId.trim()));
            } catch (NumberFormatException ignored) {
                // 解析失败视为首次加载
            }
        }
        return query;
    }

    @RepeatSubmit
    @PostMapping
    @Operation(summary = "新增动态", description = "发布朋友圈动态")
    public Result<Void> add(@Valid @RequestBody TalkAddDTO dto) {
        talkService.add(dto);
        return ResultUtils.success();
    }

    @RepeatSubmit
    @PutMapping
    @Operation(summary = "更新动态", description = "修改动态可见范围")
    public Result<Boolean> update(@Valid @RequestBody TalkUpdateDTO dto) {
        return ResultUtils.success(talkService.updateTalk(dto));
    }

    @RepeatSubmit
    @PostMapping("/like/{talkId}")
    @Operation(summary = "动态点赞", description = "对动态进行点赞")
    public Result<Void> like(@NotNull(message = "动态id不可为空") @PathVariable Long talkId) {
        talkService.like(talkId);
        return ResultUtils.success();
    }

    @RepeatSubmit
    @DeleteMapping("/comment/{commentId}")
    @Operation(summary = "删除动态评论", description = "删除自己的评论")
    public Result<Void> deleteComment(@NotNull(message = "评论id不可为空") @PathVariable Long commentId) {
        talkService.deleteComment(commentId);
        return ResultUtils.success();
    }

    @RepeatSubmit
    @PostMapping("/comment")
    @Operation(summary = "新增动态评论", description = "对动态进行评论或回复评论")
    public Result<Long> addComment(@Valid @RequestBody TalkCommentDTO dto) {
        return ResultUtils.success(talkService.addComment(dto));
    }

    @RepeatSubmit
    @DeleteMapping("/like/{talkId}")
    @Operation(summary = "取消点赞", description = "取消对动态的点赞")
    public Result<Void> unlike(@NotNull(message = "动态id不可为空") @PathVariable Long talkId) {
        talkService.unlike(talkId);
        return ResultUtils.success();
    }

    @GetMapping("/loadOfflineTalkRemind")
    @Operation(summary = "拉取未读动态提醒", description = "拉取未读的点赞和评论数量及最新提醒信息")
    public Result<TalkRemindVO> loadOfflineTalkRemind() {
        return ResultUtils.success(talkService.loadOfflineTalkRemind());
    }

    @PostMapping("/notify/readed")
    @Operation(summary = "已读动态提醒", description = "标记已读动态，用于清除未读提醒")
    public Result<Void> markReadNotify() {
        talkService.markReadNotify();
        return ResultUtils.success();
    }

    @RepeatSubmit
    @DeleteMapping("/{talkId}")
    @Operation(summary = "删除动态", description = "删除朋友圈动态")
    public Result<Void> delete(@NotNull(message = "动态id不可为空") @PathVariable Long talkId) {
        talkService.deleteTalk(talkId);
        return ResultUtils.success();
    }
}
