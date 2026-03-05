package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.Sticker;
import com.bx.implatform.vo.StickerVO;

import java.util.List;

public interface StickerService extends IService<Sticker> {

    /**
     * 根据专辑ID查询表情包列表
     *
     * @param albumId 专辑ID
     * @return 表情包列表
     */
    List<StickerVO> findByAlbumId(Long albumId);

    /**
     * 根据名称搜索表情包
     *
     * @param name 表情名称
     * @return 表情包列表
     */
    List<StickerVO> searchByName(String name);
}


