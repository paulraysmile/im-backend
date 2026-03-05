package com.bx.implatform.controller;

import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.StickerAlbumService;
import com.bx.implatform.service.StickerCustomService;
import com.bx.implatform.service.StickerService;
import com.bx.implatform.vo.StickerAlbumVO;
import com.bx.implatform.vo.StickerCustomVO;
import com.bx.implatform.vo.StickerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "表情包")
@RestController
@RequestMapping("/sticker")
@RequiredArgsConstructor
public class StickerController {

    private final StickerAlbumService stickerAlbumService;
    private final StickerService stickerService;
    private final StickerCustomService stickerFavService;

    @Operation(summary = "获取所有表情包专辑", description = "获取所有已上架的表情包专辑")
    @GetMapping("/albums")
    public Result<List<StickerAlbumVO>> findAllAlbums() {
        return ResultUtils.success(stickerAlbumService.findAllAlbums());
    }

    @Operation(summary = "查询单个专辑下的所有表情包", description = "根据专辑ID查询表情包列表")
    @GetMapping("/stickers/{albumId}")
    public Result<List<StickerVO>> findStickersByAlbum(
        @NotNull(message = "专辑ID不能为空") @PathVariable Long albumId) {
        return ResultUtils.success(stickerService.findByAlbumId(albumId));
    }

    @Operation(summary = "搜索表情包", description = "根据名称关键字搜索表情包")
    @GetMapping("/stickers/search")
    public Result<List<StickerVO>> searchByName(@RequestParam String name) {
        return ResultUtils.success(stickerService.searchByName(name));
    }

    @Operation(summary = "查询用户自定义表情列表", description = "查询用户自定义表情列表")
    @GetMapping("/custom/list")
    public Result<List<StickerCustomVO>> findAllCustomSticker() {
        return ResultUtils.success(stickerFavService.findAll());
    }

    @Operation(summary = "添加用户自定义表情", description = "添加用户自定义表情")
    @PostMapping("/custom/add")
    public Result<Void> addCustomSticker(@RequestBody @Valid StickerVO sticker) {
        stickerFavService.add(sticker);
        return ResultUtils.success();
    }

    @Operation(summary = "删除用户自定义表情", description = "删除用户自定义表情")
    @DeleteMapping("/custom/delete/{id}")
    public Result<Void> deleteCustomSticker(@NotNull(message = "自定义表情id不能为空") @PathVariable Long id) {
        stickerFavService.delete(id);
        return ResultUtils.success();
    }

    @Operation(summary = "置顶用户自定义表情", description = "置顶用户自定义表情")
    @PutMapping("/custom/top/{id}")
    public Result<Void> setCustomStickerTop(@NotNull(message = "自定义表情id不能为空") @PathVariable Long id) {
        stickerFavService.setTop(id);
        return ResultUtils.success();
    }
}
