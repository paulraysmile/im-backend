package com.bx.implatform.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.dto.UserComplaintDTO;
import com.bx.implatform.entity.UserComplaint;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.UserComplaintMapper;
import com.bx.implatform.service.UserComplainService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.UploadImageVO;
import com.bx.implatform.vo.UserComplaintVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Blue
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserComplainServiceImpl extends ServiceImpl<UserComplaintMapper, UserComplaint>
    implements UserComplainService {

    @Override
    public void initiate(UserComplaintDTO dto) {
        UserSession session = SessionContext.getSession();
        UserComplaint complaint = BeanUtils.copyProperties(dto, UserComplaint.class);
        complaint.setUserId(session.getUserId());
        complaint.setImages(JSON.toJSONString(dto.getImages()));
        this.save(complaint);
    }

    @Override
    public List<UserComplaintVO> findPage(Long maxPageId, Integer status) {
        UserSession session = SessionContext.getSession();
        LambdaQueryWrapper<UserComplaint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(UserComplaint::getUserId, session.getUserId());
        wrapper.lt(maxPageId > 0, UserComplaint::getId, maxPageId);
        wrapper.eq(status > 0, UserComplaint::getStatus, status);
        wrapper.orderByDesc(UserComplaint::getId);
        // 只去需要展示的字段，节约性能
//        wrapper.select(UserComplaint::getId, UserComplaint::getCreateTime, UserComplaint::getTargetName,
//            UserComplaint::getType,UserComplaint::getContent, UserComplaint::getResolvedType, UserComplaint::getResolvedTime);
        // 一页最多取10条
        wrapper.last("limit 10");
        List<UserComplaint> complaints = this.list(wrapper);
        // vo转换
        return complaints.stream().map(o -> BeanUtils.copyProperties(o, UserComplaintVO.class))
            .collect(Collectors.toList());
    }

    @Override
    public UserComplaintVO findById(Long id) {
        UserSession session = SessionContext.getSession();
        LambdaQueryWrapper<UserComplaint> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(UserComplaint::getUserId, session.getUserId());
        wrapper.eq(UserComplaint::getId, id);
        UserComplaint complaint = this.getById(id);
        if (Objects.isNull(complaint)) {
            throw new GlobalException("投诉信息不存在");
        }
        UserComplaintVO vo = BeanUtils.copyProperties(complaint, UserComplaintVO.class);
        vo.setImages(JSON.parseArray(complaint.getImages(), UploadImageVO.class));
        return vo;
    }
}
