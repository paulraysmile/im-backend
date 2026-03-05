package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户自定义表情包
 *
 * @author Blue
 * @date 2025-12-31
 */
@Data
@TableName("im_sticker_custom")
public class StickerCustom {

    /**
     * 表情id
     */
    @TableId
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 专辑id
     */
    private Long albumId;

    /**
     * 表情id
     */
    private Long stickerId;

    /**
     * 表情名称
     */
    private String name;

    /**
     * 表情图片url
     */
    private String imageUrl;

    /**
     * 缩略图url
     */
    private String thumbUrl;

    /**
     * 图片宽度
     */
    private Integer width;

    /**
     * 图片高度
     */
    private Integer height;

    /**
     * 排序权重,序号小的展示在前面
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

}

