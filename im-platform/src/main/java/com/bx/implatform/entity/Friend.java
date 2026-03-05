package com.bx.implatform.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * <p>
 * 好友
 * </p>
 *
 * @author blue
 * @since 2022-10-22
 */
@Data
@TableName("im_friend")
public class Friend{

    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 好友id
     */
    private Long friendId;

    /**
     * 好友昵称
     */
    private String friendNickName;

    /**
     * 好友头像
     */
    private String friendHeadImage;

    /**
     * 好友所属企业名称
     */
    private String friendCompanyName;

    /**
     * 备注昵称
     */
    private String remarkNickName;

    /**
     * 是否开启免打扰
     */
    private Boolean isDnd;

    /**
     * 是否置顶会话
     */
    private Boolean isTop;

    /**
     * 是否已删除
     */
    private Boolean deleted;

    /**
     * 创建时间
     */
    private Date createdTime;

    public String getShowNickName() {
        return StrUtil.blankToDefault(remarkNickName, friendNickName);
    }


}
