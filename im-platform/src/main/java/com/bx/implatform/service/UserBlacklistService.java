package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.UserBlacklist;

public interface UserBlacklistService extends IService<UserBlacklist> {

    /**
     * 添加到黑名单
     *
     * @param fromUserId 拉黑用户id
     * @param toUserId   被拉黑用户id
     */
    void add(Long fromUserId, Long toUserId);

    /**
     * 从黑名单中移除
     *
     * @param fromUserId 拉黑用户id
     * @param toUserId   被拉黑用户id
     */
    void remove(Long fromUserId, Long toUserId);

    /**
     * 判断是否已经拉黑对方
     *
     * @param fromUserId 拉黑用户id
     * @param toUserId   被拉黑用户id
     * @return boolean
     */
    Boolean isInBlacklist(Long fromUserId, Long toUserId);

}
