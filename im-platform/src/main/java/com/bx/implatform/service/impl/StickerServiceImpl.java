package com.bx.implatform.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.Sticker;
import com.bx.implatform.mapper.StickerMapper;
import com.bx.implatform.service.StickerAlbumService;
import com.bx.implatform.service.StickerService;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.StickerAlbumVO;
import com.bx.implatform.vo.StickerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = RedisKey.IM_CACHE_STICKER_STICKERS)
public class StickerServiceImpl extends ServiceImpl<StickerMapper, Sticker> implements StickerService {

    private final StickerAlbumService stickerAlbumService;

    @Cacheable(key = "#albumId")
    @Override
    public List<StickerVO> findByAlbumId(Long albumId) {
        LambdaQueryWrapper<Sticker> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Sticker::getAlbumId, albumId);
        wrapper.orderByAsc(Sticker::getSort);
        List<Sticker> stickers = this.list(wrapper);
        return stickers.stream().map(sticker -> BeanUtils.copyProperties(sticker, StickerVO.class))
            .collect(Collectors.toList());
    }

    @Override
    public List<StickerVO> searchByName(String name) {
        // 过滤掉未上架的专辑
        List<StickerAlbumVO> albums = stickerAlbumService.findAllAlbums();
        List<Long> albumIds = albums.stream().map(StickerAlbumVO::getId).collect(Collectors.toList());
        if(CollectionUtil.isEmpty(albumIds)){
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Sticker> wrapper = Wrappers.lambdaQuery();
        wrapper.in(Sticker::getAlbumId,albumIds);
        wrapper.like(Sticker::getName, name);
        wrapper.last("limit 20");
        List<Sticker> stickers = this.list(wrapper);
        return stickers.stream().map(sticker -> BeanUtils.copyProperties(sticker, StickerVO.class))
            .collect(Collectors.toList());
    }
}


