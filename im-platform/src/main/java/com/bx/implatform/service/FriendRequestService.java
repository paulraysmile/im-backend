package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.FriendRequestApplyDTO;
import com.bx.implatform.entity.FriendRequest;
import com.bx.implatform.vo.FriendRequestVO;

import java.util.List;

public interface FriendRequestService  extends IService<FriendRequest> {

    /**
     * 加载最近未处理的请求
     * @return
     */
    List<FriendRequestVO> loadPendingList();

    /**
     * 好友添加申请
     * @param dto dto
     * @return
     */
    FriendRequestVO apply(FriendRequestApplyDTO dto);

    /**
     * 同意好友申请
     * @param id 好友申请id
     * @return
     */
    void approve(Long id);

    /**
     * 拒绝好友申请
     * @param id 好友申请id
     * @return
     */
    void reject(Long id);


    /**
     * 撤回好友申请
     * @param id 好友申请id
     * @return
     */
    void recall(Long id);


}
