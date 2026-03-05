package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.UserComplaintDTO;
import com.bx.implatform.entity.UserComplaint;
import com.bx.implatform.vo.UserComplaintVO;

import java.util.List;

;

public interface UserComplainService extends IService<UserComplaint> {

    /**
     * 用户发起投诉
     * @param dto
     */
    void initiate(UserComplaintDTO dto);

    /**
     * 分页拉取投诉列表，一次拉取10条
     * @param maxPageId 分页最大id
     * @param status 处理状态
     * @return
     */
    List<UserComplaintVO> findPage(Long maxPageId, Integer status);

    /**
     * 查询投诉信息
     * @param id 投诉id
     * @return
     */
    UserComplaintVO findById(Long id);
}
