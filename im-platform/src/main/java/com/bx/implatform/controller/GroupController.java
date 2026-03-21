package com.bx.implatform.controller;

import com.bx.implatform.annotation.RepeatSubmit;
import com.bx.implatform.dto.*;
import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.GroupService;
import com.bx.implatform.vo.BizTokenVO;
import com.bx.implatform.vo.GroupMemberVO;
import com.bx.implatform.vo.GroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "群聊")
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @RepeatSubmit
    @Operation(summary = "创建群聊", description = "创建群聊")
    @PostMapping("/create")
    public Result<GroupVO> createGroup(@Valid @RequestBody GroupVO vo) {
        return ResultUtils.success(groupService.createGroup(vo));
    }

    @RepeatSubmit
    @Operation(summary = "修改群聊信息", description = "修改群聊信息")
    @PutMapping("/modify")
    public Result<GroupVO> modifyGroup(@Valid @RequestBody GroupVO vo) {
        return ResultUtils.success(groupService.modifyGroup(vo));
    }

    @RepeatSubmit
    @Operation(summary = "解散群聊", description = "解散群聊")
    @DeleteMapping("/delete/{groupId}")
    public Result deleteGroup(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
        return ResultUtils.success();
    }

    @Operation(summary = "查询群聊", description = "查询单个群聊信息")
    @GetMapping("/find/{groupId}")
    public Result<GroupVO> findGroup(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        return ResultUtils.success(groupService.findById(groupId));
    }

    @Operation(summary = "查询群聊列表", description = "查询群聊列表")
    @GetMapping("/list")
    public Result<List<GroupVO>> findGroups() {
        return ResultUtils.success(groupService.findGroups());
    }

    @RepeatSubmit
    @Operation(summary = "邀请进群", description = "邀请好友进群")
    @PostMapping("/invite")
    public Result invite(@Valid @RequestBody GroupInviteDTO dto) {
        groupService.invite(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "生成名片分享token", description = "生成名片分享token")
    @GetMapping("/card/token/{groupId}")
    public Result<BizTokenVO> generateShareCardToken(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        return ResultUtils.success(groupService.generateShareCardToken(groupId));
    }

    @Operation(summary = "生成二维码token", description = "生成群聊二维码token")
    @GetMapping("/qrcode/token/{groupId}")
    public Result<BizTokenVO> generateQrcodeToken(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        return ResultUtils.success(groupService.generateQrcodeToken(groupId));
    }

    @RepeatSubmit
    @Operation(summary = "主动申请进入群聊", description = "通过token加入群聊（需要先获取token）")
    @PostMapping("/join")
    public Result<GroupVO> join(@Valid @RequestBody GroupJoinDTO dto) {
        return ResultUtils.success(groupService.join(dto));
    }

    @Operation(summary = "查询群聊成员", description = "查询群聊成员")
    @GetMapping("/members/{groupId}")
    public Result<List<GroupMemberVO>> findGroupMembers(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId,
        @RequestParam(defaultValue = "0") Long version) {
        return ResultUtils.success(groupService.findGroupMembers(groupId, version));
    }

    @Operation(summary = "查询在线群聊成员id", description = "查询在线群聊成员id")
    @GetMapping("/members/online/{groupId}")
    public Result<List<Long>> findOnlineMemberIds(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId){
        return ResultUtils.success(groupService.findOnlineMemberIds(groupId));
    }

    @RepeatSubmit
    @Operation(summary = "将成员移出群聊", description = "将成员移出群聊")
    @DeleteMapping("/members/remove")
    public Result removeMembers(@Valid @RequestBody GroupMemberRemoveDTO dto) {
        groupService.removeGroupMembers(dto);
        return ResultUtils.success();
    }

    @RepeatSubmit
    @Operation(summary = "退出群聊", description = "退出群聊")
    @DeleteMapping("/quit/{groupId}")
    public Result quitGroup(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        groupService.quitGroup(groupId);
        return ResultUtils.success();
    }


    @RepeatSubmit
    @Operation(summary = "开启/关闭全员禁言", description = "开启/关闭全员禁言")
    @PutMapping("/muted")
    public Result setGroupMuted(@Valid @RequestBody GroupMutedDTO dto) {
        groupService.setGroupMuted(dto);
        return ResultUtils.success();
    }

    @RepeatSubmit
    @Operation(summary = "对群成员开启/关闭禁言", description = "对群成员开启/关闭禁言")
    @PutMapping("/members/muted")
    public Result setMemberMuted(@Valid @RequestBody GroupMemberMutedDTO dto) {
        groupService.setMemberMuted(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "新增群置顶消息", description = "新增群置顶消息")
    @PostMapping("/setTopMessage/{groupId}")
    public Result setTopMessage(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId,
        @NotNull(message = "消息id不能为空") @RequestParam Long messageId) {
        groupService.setTopMessage(groupId, messageId);
        return ResultUtils.success();
    }

    @Operation(summary = "移除群置顶消息", description = "移除群置顶消息,对所有群成员生效")
    @DeleteMapping("/removeTopMessage/{groupId}")
    public Result removeTopMessage(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        groupService.removeTopMessage(groupId);
        return ResultUtils.success();
    }

    @Operation(summary = "隐藏群置顶消息", description = "隐藏置顶消息,仅对自己生效")
    @DeleteMapping("/hideTopMessage/{groupId}")
    public Result hideTopMessage(@NotNull(message = "群聊id不能为空") @PathVariable Long groupId) {
        groupService.hideTopMessage(groupId);
        return ResultUtils.success();
    }

    @Operation(summary = "新增管理员", description = "新增管理员")
    @PostMapping("/manager/add")
    public Result addManager(@Valid @RequestBody GroupManagerDTO dto) {
        groupService.addManager(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "移除管理员", description = "移除管理员")
    @DeleteMapping("/manager/remove")
    public Result removeManager(@Valid @RequestBody GroupManagerDTO dto) {
        groupService.removeManager(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "开启/关闭免打扰", description = "开启/关闭免打扰")
    @PutMapping("/dnd")
    public Result setGroupDnd(@Valid @RequestBody GroupDndDTO dto) {
        groupService.setDnd(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "开启/关闭会话置顶", description = "开启/关闭会话置顶")
    @PutMapping("/top")
    public Result setGroupTop(@Valid @RequestBody GroupTopDTO dto) {
        groupService.setTop(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "设置是否允许普通成员邀请好友", description = "设置是否允许普通成员邀请好友")
    @PutMapping("/allowInvite")
    public Result setAllowInvite(@Valid @RequestBody GroupAllowInviteDTO dto) {
        groupService.setAllowInvite(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "设置是否允许普通成员分享名片", description = "设置是否允许普通成员分享名片")
    @PutMapping("/allowShareCard")
    public Result setAllowShareCard(@Valid @RequestBody GroupAllowShareCardDTO dto) {
        groupService.setAllowShareCard(dto);
        return ResultUtils.success();
    }

    @Operation(summary = "设置是否允许普通成员群内互相加好友", description = "设置是否允许普通成员群内互相加好友")
    @PutMapping("/allowAddOther")
    public Result setAllowAddOther(@Valid @RequestBody GroupAllowAddOtherDTO dto) {
        groupService.setAllowAddOther(dto);
        return ResultUtils.success();
    }

}

