package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.StickerAlbum;
import com.bx.implatform.vo.StickerAlbumVO;

import java.util.List;

public interface StickerAlbumService extends IService<StickerAlbum> {

    /**
     * 获取所有上架且未删除的表情包专辑
     *
     * @return 专辑列表
     */
    List<StickerAlbumVO> findAllAlbums();
}


