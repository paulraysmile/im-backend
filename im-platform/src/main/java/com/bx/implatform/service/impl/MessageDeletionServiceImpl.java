package com.bx.implatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.entity.MessageDeletion;
import com.bx.implatform.enums.ChatType;
import com.bx.implatform.enums.DeleteType;
import com.bx.implatform.mapper.MessageDeletionMapper;
import com.bx.implatform.service.MessageDeletionService;
import com.bx.implatform.session.SessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Blue
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDeletionServiceImpl extends ServiceImpl<MessageDeletionMapper, MessageDeletion>
    implements MessageDeletionService {

    @Override
    public void deleteByMessage(ChatType chatType, Long chatId, List<Long> messageIds) {
        //int a = 1/0;
        Long userId = SessionContext.getSession().getUserId();
        // 过滤已经删除的消息
        LambdaQueryWrapper<MessageDeletion> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MessageDeletion::getUserId, userId);
        wrapper.eq(MessageDeletion::getChatId, chatId);
        wrapper.eq(MessageDeletion::getChatType, chatType.getCode());
        wrapper.in(MessageDeletion::getMessageId, messageIds);
        wrapper.select(MessageDeletion::getMessageId);
        List<MessageDeletion> deletions = this.list(wrapper);
        List<Long> existIds = deletions.stream().map(MessageDeletion::getMessageId).collect(Collectors.toList());
        // 存储删除记录
        List<MessageDeletion> newDeletions =
            messageIds.stream().filter(id -> existIds.stream().noneMatch(existId -> id.equals(existId))).map(id -> {
                MessageDeletion deletion = new MessageDeletion();
                deletion.setMessageId(id);
                deletion.setDeleteType(DeleteType.BY_MESSAGE.getCode());
                deletion.setChatType(chatType.getCode());
                deletion.setChatId(chatId);
                deletion.setUserId(userId);
                deletion.setDeleteTime(new Date());
                return deletion;
            }).collect(Collectors.toList());
        this.saveBatch(newDeletions);
    }

    @Transactional
    @Override
    public void deleteByChat(ChatType chatType, Long chatId, Long maxMessageId) {
        Long userId = SessionContext.getSession().getUserId();
        // 清理该会话之前删除的消息记录(整个会话都删了，之前的删除记录没作用了)
        LambdaQueryWrapper<MessageDeletion> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MessageDeletion::getUserId, userId);
        wrapper.eq(MessageDeletion::getChatId, chatId);
        wrapper.eq(MessageDeletion::getChatType, chatType.getCode());
        wrapper.le(MessageDeletion::getMessageId, maxMessageId);
        this.remove(wrapper);
        // 存储新的删除记录
        MessageDeletion deletion = new MessageDeletion();
        deletion.setMessageId(maxMessageId);
        deletion.setDeleteType(DeleteType.BY_CHAT.getCode());
        deletion.setChatType(chatType.getCode());
        deletion.setChatId(chatId);
        deletion.setUserId(userId);
        deletion.setDeleteTime(new Date());
        this.save(deletion);
    }

    @Override
    public List<MessageDeletion> findByChatType(ChatType chatType, Date minDate) {
        Long userId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<MessageDeletion> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MessageDeletion::getUserId, userId);
        wrapper.eq(MessageDeletion::getChatType, chatType.getCode());
        wrapper.ge(MessageDeletion::getDeleteTime, minDate);
        return this.list(wrapper);
    }


}
