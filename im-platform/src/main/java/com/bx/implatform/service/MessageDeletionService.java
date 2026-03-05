package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.MessageDeletion;
import com.bx.implatform.enums.ChatType;

import java.util.Date;
import java.util.List;

/**
 * 消息删除记录Service
 *
 * @author Blue
 * @date 2025-12-31
 */
public interface MessageDeletionService extends IService<MessageDeletion> {

    /**
     * 删除指定id的消息
     *
     * @param chatType   会话类型
     * @param chatId     会话id
     * @param messageIds 消息id列表
     */
    void deleteByMessage(ChatType chatType, Long chatId, List<Long> messageIds);

    /**
     * 按会话删除消息
     *
     * @param chatType     会话类型
     * @param chatId       会话id
     * @param maxMessageId 会话最大消息id
     */
    void deleteByChat(ChatType chatType, Long chatId, Long maxMessageId);

    /**
     * 查询删除记录
     * @param chatType 会话类型
     * @param minDate 最小的删除时间
     * @return
     */
    List<MessageDeletion> findByChatType(ChatType chatType, Date minDate);

}

