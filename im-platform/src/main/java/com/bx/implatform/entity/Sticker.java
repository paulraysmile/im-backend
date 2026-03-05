package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 表情包
 *
 * @author blue
 */
@Data
@TableName("im_sticker")
public class Sticker {

    /**
     * 表情ID
     */
    @TableId
    private Long id;

    /**
     * 专辑ID
     */
    private Long albumId;

    /**
     * 表情名称
     */
    private String name;

    /**
     * 表情图片URL
     */
    private String imageUrl;

    /**
     * 缩略图URL
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
     * 排序权重
     */
    private Integer sort;

    /**
     * 删除标识  0：正常   1：已删除
     */
    @TableLogic
    private Boolean deleted;

    /**
     * 创建者
     */
    private Long creator;

    /**
     * 创建时间
     */
    private Date createTime;
}


