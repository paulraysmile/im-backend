package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.config.props.MinioProperties;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.entity.FileInfo;
import com.bx.implatform.enums.FileType;
import com.bx.implatform.enums.ResultCode;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.FileInfoMapper;
import com.bx.implatform.service.FileService;
import com.bx.implatform.thirdparty.MinioService;
import com.bx.implatform.util.FileUtil;
import com.bx.implatform.util.ImageUtil;
import com.bx.implatform.vo.UploadImageVO;
import com.bx.implatform.vo.UploadVideoVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * 文件上传服务
 * author: Blue
 * date: 2024-09-28 version: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileService {

    private final MinioService minioSerivce;

    private final MinioProperties minioProps;

    @PostConstruct
    public void init() {
        if (!minioSerivce.bucketExists(minioProps.getBucketName())) {
            // 创建bucket
            minioSerivce.makeBucket(minioProps.getBucketName());
            // 公开bucket
            minioSerivce.setBucketPublic(minioProps.getBucketName());
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            // 文件名长度校验
            checkFileNameLength(file);
            // 大小校验
            if (file.getSize() > Constant.MAX_FILE_SIZE) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "文件大小不能超过20M");
            }
            // 如果文件已存在，直接复用
            String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
            FileInfo fileInfo = findByMd5(md5, FileType.FILE.code());
            if (!Objects.isNull(fileInfo)) {
                // 更新上传时间
                fileInfo.setUploadTime(new Date());
                this.updateById(fileInfo);
                // 返回
                return fileInfo.getFilePath();
            }
            // 上传
            String fileName = minioSerivce.upload(minioProps.getBucketName(), minioProps.getFilePath(), file);
            if (StringUtils.isEmpty(fileName)) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "文件上传失败");
            }
            String url = generUrl(FileType.FILE, fileName);
            // 保存文件
            saveFileInfo(file, md5, url);
            log.info("文件文件成功,url:{}", url);
            return url;
        } catch (IOException e) {
            log.error("上传图片失败，{}", e.getMessage(), e);
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "上传图片失败");
        }
    }

    @Transactional
    @Override
    public UploadImageVO uploadImage(MultipartFile file, Boolean isPermanent,Long thumbSize) {
        try {
            // 文件名长度校验
            checkFileNameLength(file);
            // 大小校验
            if (file.getSize() > Constant.MAX_IMAGE_SIZE) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片大小不能超过20M");
            }
            // 图片格式校验
            if (!FileUtil.isImage(file.getOriginalFilename())) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片格式不合法");
            }
            UploadImageVO vo = new UploadImageVO();
            // 获取图片长度和宽度
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (!Objects.isNull(bufferedImage)) {
                vo.setWidth(bufferedImage.getWidth());
                vo.setHeight(bufferedImage.getHeight());
            }
            // 如果文件已存在，直接复用
            String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
            FileInfo fileInfo = findByMd5(md5, FileType.IMAGE.code());
            if (!Objects.isNull(fileInfo)) {
                // 更新上传时间和持久化标记
                fileInfo.setIsPermanent(isPermanent || fileInfo.getIsPermanent());
                fileInfo.setUploadTime(new Date());
                this.updateById(fileInfo);
                // 返回
                vo.setOriginUrl(fileInfo.getFilePath());
                vo.setThumbUrl(fileInfo.getCompressedPath());
                return vo;
            }
            // 上传原图
            String fileName = minioSerivce.upload(minioProps.getBucketName(), minioProps.getImagePath(), file);
            if (StringUtils.isEmpty(fileName)) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片上传失败");
            }
            vo.setOriginUrl(generUrl(FileType.IMAGE, fileName));
            if (file.getSize() > thumbSize * 1024) {
                // 大于缩略图的文件需上传缩略图
                byte[] imageByte = ImageUtil.compressForScale(file.getBytes(), thumbSize);
                String thumbFileName = minioSerivce.upload(minioProps.getBucketName(), minioProps.getImagePath(),
                    file.getOriginalFilename(), imageByte, file.getContentType());
                if (StringUtils.isEmpty(thumbFileName)) {
                    throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片上传失败");
                }
                vo.setThumbUrl(generUrl(FileType.IMAGE, thumbFileName));
                // 保存文件信息
                saveImageFileInfo(file, md5, vo.getOriginUrl(), vo.getThumbUrl(), isPermanent);
            } else {
                // 小于50k，用原图充当缩略图
                vo.setThumbUrl(generUrl(FileType.IMAGE, fileName));
                // 保存文件信息,由于缩略图不允许删除，此时原图也不允许删除
                saveImageFileInfo(file, md5, vo.getOriginUrl(), vo.getThumbUrl(), true);
            }
            log.info("文件图片成功,url:{}", vo.getOriginUrl());
            return vo;
        } catch (IOException e) {
            log.error("上传图片失败，{}", e.getMessage(), e);
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "图片上传失败");
        }
    }

    @Override
    public UploadVideoVO uploadVideo(MultipartFile file) {
        try {
            // 文件名长度校验
            checkFileNameLength(file);
            // 大小校验
            if (file.getSize() > Constant.MAX_VIDEO_SIZE) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "视频文件大小不能超过50M");
            }
            UploadVideoVO vo = new UploadVideoVO();
            // 视频的宽高
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getInputStream());
            grabber.start();
            vo.setWidth(grabber.getImageWidth());
            vo.setHeight(grabber.getImageHeight());
            grabber.stop();
            // 如果文件已存在，直接复用
            String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
            FileInfo fileInfo = findByMd5(md5, FileType.VIDEO.code());
            if (!Objects.isNull(fileInfo)) {
                // 更新上传时间
                fileInfo.setUploadTime(new Date());
                this.updateById(fileInfo);
                // 返回
                vo.setVideoUrl(fileInfo.getFilePath());
                vo.setCoverUrl(fileInfo.getCoverPath());
                return vo;
            }
            // 上传
            String fileName = minioSerivce.upload(minioProps.getBucketName(), minioProps.getVideoPath(), file);
            if (StringUtils.isEmpty(fileName)) {
                throw new GlobalException(ResultCode.PROGRAM_ERROR, "视频上传失败");
            }
            String url = generUrl(FileType.VIDEO, fileName);
            String coverName = createCoverImage(file);
            vo.setVideoUrl(url);
            vo.setCoverUrl(generUrl(FileType.VIDEO, coverName));
            saveVideoFileInfo(file, md5, vo.getVideoUrl(), vo.getCoverUrl());
            log.info("上传视频成功,url:{}", url);
            return vo;
        } catch (IOException e) {
            log.error("上传视频失败，{}", e.getMessage(), e);
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "视频上传失败");
        }
    }

    private String createCoverImage(MultipartFile file) {
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file.getInputStream());
            grabber.start();
            // 读取第一帧
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage firstFrame = converter.convert(grabber.grabImage());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(firstFrame, "PNG", outputStream);
            // 压缩
            byte[] imageByte = ImageUtil.compressForScale(outputStream.toByteArray(), 150);
            String fileName = FileUtil.excludeExtension(file.getOriginalFilename()) + "_cover.png";
            String imageName =
                minioSerivce.upload(minioProps.getBucketName(), minioProps.getVideoPath(), fileName, imageByte,
                    "image/png");
            grabber.stop();
            outputStream.close();
            return imageName;
        } catch (IOException e) {
            throw new GlobalException(ResultCode.PROGRAM_ERROR, "视频上传失败");
        }
    }

    private String generUrl(FileType fileType, String fileName) {
        return StrUtil.join("/", minioProps.getDomain(), minioProps.getBucketName(), getBucketPath(fileType), fileName);
    }

    private String getBucketPath(FileType fileType) {
        return switch (fileType) {
            case FILE -> minioProps.getFilePath();
            case IMAGE -> minioProps.getImagePath();
            case VIDEO -> minioProps.getVideoPath();
        };
    }

    private FileInfo findByMd5(String md5, Integer fileType) {
        LambdaQueryWrapper<FileInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FileInfo::getMd5, md5);
        wrapper.eq(FileInfo::getFileType, fileType);
        wrapper.last("limit 1");
        return getOne(wrapper);
    }

    private void saveImageFileInfo(MultipartFile file, String md5, String filePath, String compressedPath,
        Boolean isPermanent) throws IOException {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(file.getOriginalFilename());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(FileType.IMAGE.code());
        fileInfo.setFilePath(filePath);
        fileInfo.setCompressedPath(compressedPath);
        fileInfo.setMd5(md5);
        fileInfo.setIsPermanent(isPermanent);
        fileInfo.setUploadTime(new Date());
        this.save(fileInfo);
    }

    private void saveVideoFileInfo(MultipartFile file, String md5, String filePath, String coverPath)
        throws IOException {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(file.getOriginalFilename());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(FileType.VIDEO.code());
        fileInfo.setFilePath(filePath);
        fileInfo.setCoverPath(coverPath);
        fileInfo.setMd5(md5);
        fileInfo.setIsPermanent(false);
        fileInfo.setUploadTime(new Date());
        this.save(fileInfo);
    }

    private void saveFileInfo(MultipartFile file, String md5, String filePath) throws IOException {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(file.getOriginalFilename());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(FileType.FILE.code());
        fileInfo.setFilePath(filePath);
        fileInfo.setMd5(md5);
        fileInfo.setIsPermanent(false);
        fileInfo.setUploadTime(new Date());
        this.save(fileInfo);
    }

    private void checkFileNameLength(MultipartFile file){
        if(file.getOriginalFilename().length() > Constant.MAX_FILE_NAME_LENGTH){
            throw new GlobalException("文件名长度不能超过" + Constant.MAX_FILE_NAME_LENGTH);
        }
    }
}
