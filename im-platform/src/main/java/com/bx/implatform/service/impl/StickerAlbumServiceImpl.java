package com.bx.implatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.StickerAlbum;
import com.bx.implatform.mapper.StickerAlbumMapper;
import com.bx.implatform.service.StickerAlbumService;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.StickerAlbumVO;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = RedisKey.IM_CACHE_STICKER_ALBUMS)
public class StickerAlbumServiceImpl extends ServiceImpl<StickerAlbumMapper, StickerAlbum>
    implements StickerAlbumService {

    @Cacheable
    @Override
    public List<StickerAlbumVO> findAllAlbums() {
        LambdaQueryWrapper<StickerAlbum> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StickerAlbum::getStatus, true);
        wrapper.orderByAsc(StickerAlbum::getSort);
        List<StickerAlbum> albums = this.list(wrapper);
        return albums.stream().map(album -> BeanUtils.copyProperties(album, StickerAlbumVO.class))
            .collect(Collectors.toList());
    }
}


