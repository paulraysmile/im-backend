package com.bx.implatform.controller;

import com.bx.implatform.result.Result;
import com.bx.implatform.result.ResultUtils;
import com.bx.implatform.service.FileService;
import com.bx.implatform.vo.UploadImageVO;
import com.bx.implatform.vo.UploadVideoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@Tag(name = "文件上传")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传图片", description = "上传图片,上传后返回原图和缩略图的url")
    @PostMapping("/image/upload")
    public Result<UploadImageVO> uploadImage(@RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "true") Boolean isPermanent, @RequestParam(defaultValue = "50") Long thumbSize) {
        return ResultUtils.success(fileService.uploadImage(file, isPermanent, thumbSize));
    }

    @Operation(summary = "上传视频", description = "上传视频,上传后返回视频和封面的url")
    @PostMapping("/video/upload")
    public Result<UploadVideoVO> uploadVideo(@RequestParam("file") MultipartFile file) {
        return ResultUtils.success(fileService.uploadVideo(file));
    }

    @CrossOrigin
    @Operation(summary = "上传文件", description = "上传文件，上传后返回文件url")
    @PostMapping("/file/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResultUtils.success(fileService.uploadFile(file), "");
    }

}