package com.bx.implatform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 朋友圈动态实体
 * <p>
 * 存储用户发布的朋友圈动态，支持文字、图片、视频、定位等
 * </p>
 *
 * @author im-platform
 */
@Data
@TableName("im_talk")
public class Talk {

    @TableId
    private Long id;

    /**
     * 发布者用户id
     */
    private Long userId;

    /**
     * 动态内容
     */
    private String content;

    /**
     * 可见范围: 1-私密 2-好友可见 9-公开
     */
    private Integer visibleScope;

    /**
     * 地址
     */
    private String address;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 附件列表JSON，格式:[{fileType,url,coverUrl},...]
     */
    private String files;

    /**
     * 创建时间
     */
    private Date createTime;
}
