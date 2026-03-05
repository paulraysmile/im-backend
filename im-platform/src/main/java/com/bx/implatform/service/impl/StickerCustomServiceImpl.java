package com.bx.implatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.entity.StickerCustom;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.StickerCustomMapper;
import com.bx.implatform.service.StickerCustomService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.StickerCustomVO;
import com.bx.implatform.vo.StickerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户自定义表情包
 *
 * @author Blue
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class StickerCustomServiceImpl extends ServiceImpl<StickerCustomMapper, StickerCustom>
    implements StickerCustomService {


    @Override
    public List<StickerCustomVO> findAll() {
        UserSession session = SessionContext.getSession();
        LambdaQueryWrapper<StickerCustom> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StickerCustom::getUserId, session.getUserId());
        wrapper.orderByAsc(StickerCustom::getSort);
        wrapper.orderByDesc(StickerCustom::getId);
        List<StickerCustom> stickers = this.list(wrapper);
        return stickers.stream().map(sticker -> BeanUtils.copyProperties(sticker, StickerCustomVO.class))
            .collect(Collectors.toList());
    }

    @Override
    public void add(StickerVO sticker) {
        UserSession session = SessionContext.getSession();
        StickerCustom custom = new StickerCustom();
        custom.setAlbumId(sticker.getAlbumId());
        custom.setStickerId(sticker.getId());
        custom.setUserId(session.getUserId());
        custom.setName(sticker.getName());
        custom.setImageUrl(sticker.getImageUrl());
        custom.setThumbUrl(sticker.getThumbUrl());
        custom.setWidth(sticker.getWidth());
        custom.setHeight(sticker.getHeight());
        custom.setCreateTime(new Date());
        // 判断数量
        LambdaQueryWrapper<StickerCustom> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StickerCustom::getUserId, session.getUserId());
        if (this.count(wrapper) >= Constant.MAX_CUSTOM_STICKET_SIZE) {
            throw new GlobalException("您添加的表情数量已经达到上限");
        }
        // 查询当前用户的最大sort值，新添加的表情排在前面
        wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StickerCustom::getUserId, session.getUserId());
        wrapper.orderByDesc(StickerCustom::getSort);
        wrapper.last("limit 1");
        StickerCustom firstSticker = this.getOne(wrapper);
        if (!Objects.isNull(firstSticker)) {
            custom.setSort(firstSticker.getSort() - 1);
        } else {
            // 初始值设置一个比较大的值，这样可以不断的往前插入
            custom.setSort(10000);
        }
        this.save(custom);
    }

    @Override
    public void delete(Long id) {
        UserSession session = SessionContext.getSession();
        StickerCustom custom = this.getById(id);
        if (Objects.isNull(custom)) {
            throw new GlobalException("自定义表情不存在");
        }
        if (!custom.getUserId().equals(session.getUserId())) {
            throw new GlobalException("您没有操作权限");
        }
        this.removeById(id);
    }

    @Override
    public void setTop(Long id) {
        UserSession session = SessionContext.getSession();
        StickerCustom custom = this.getById(id);
        if (Objects.isNull(custom)) {
            throw new GlobalException("自定义表情不存在");
        }
        if (!custom.getUserId().equals(session.getUserId())) {
            throw new GlobalException("您没有操作权限");
        }
        // 查询当前用户的最小sort值
        LambdaQueryWrapper<StickerCustom> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StickerCustom::getUserId, session.getUserId());
        wrapper.orderByAsc(StickerCustom::getSort);
        wrapper.last("limit 1");
        StickerCustom firstSticker = this.getOne(wrapper);
        // 更新sort值
        LambdaUpdateWrapper<StickerCustom> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(StickerCustom::getId, id);
        updateWrapper.set(StickerCustom::getSort, firstSticker.getSort() - 1);
        this.update(updateWrapper);
    }



}
