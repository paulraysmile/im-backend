package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 表情包专辑
 *
 * @author blue
 */
@Data
@TableName("im_sticker_album")
public class StickerAlbum {

    /**
     * 专辑ID
     */
    @TableId
    private Long id;

    /**
     * 专辑名称
     */
    private String name;

    /**
     * 专辑logo
     */
    private String logoUrl;

    /**
     * 排序权重
     */
    private Integer sort;

    /**
     * 状态 0:下架 1:上架
     */
    private Boolean status;

    /**
     * 专辑描述
     */
    private String description;

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


