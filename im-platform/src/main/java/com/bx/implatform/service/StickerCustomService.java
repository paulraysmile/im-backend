package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.StickerCustom;
import com.bx.implatform.vo.StickerCustomVO;
import com.bx.implatform.vo.StickerVO;

import java.util.List;

/**
 * 用户自定义表情包
 *
 * @author Blue
 * @date 2025-12-31
 */
public interface StickerCustomService extends IService<StickerCustom> {

    /**
     * 查询用户自定义表情列表
     *
     * @return 自定义表情列表
     */
    List<StickerCustomVO> findAll();

    /**
     * 添加用户自定义表情
     *
     * @param sticker 表情信息
     */
    void add(StickerVO sticker);

    /**
     * 删除用户自定义表情
     *
     * @param id 表情id
     */
    void delete(Long id);

    /**
     * 置顶用户自定义表情
     *
     * @param id 表情id
     */
    void setTop(Long id);



}

