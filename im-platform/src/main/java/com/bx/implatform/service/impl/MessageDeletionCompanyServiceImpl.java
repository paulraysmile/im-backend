package com.bx.implatform.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.entity.MessageDeletion;
import com.bx.implatform.enums.ChatType;
import com.bx.implatform.enums.DeleteType;
import com.bx.implatform.mapper.MessageDeletionMapper;
import com.bx.implatform.service.MessageDeletionCompanyService;
import com.bx.implatform.session.SessionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDeletionCompanyServiceImpl extends ServiceImpl<MessageDeletionMapper, MessageDeletion>
    implements MessageDeletionCompanyService {

    @Override
    public void deleteByMessage(Long companyId, ChatType chatType, Long chatId, List<Long> messageIds) {
        Long userId = SessionContext.getSession().getUserId();
        // 过滤已经删除的消息
        LambdaQueryWrapper<MessageDeletion> wrapper = Wrappers.lambdaQuery();
        wrapper.select(MessageDeletion::getMessageId);
        wrapper.eq(MessageDeletion::getUserId, userId);
        wrapper.eq(MessageDeletion::getCompanyId, companyId);
        wrapper.eq(MessageDeletion::getChatId, chatId);
        wrapper.eq(MessageDeletion::getChatType, chatType.getCode());
        wrapper.in(MessageDeletion::getMessageId, messageIds);
        List<MessageDeletion> deletions = this.list(wrapper);
        // 存储删除记录
        List<MessageDeletion> newDeletions = messageIds.stream()
                .filter(id -> deletions.stream().noneMatch(m -> id.equals(m.getMessageId())))
                .map(id -> {
                    MessageDeletion deletion = new MessageDeletion();
                    deletion.setMessageId(id);
                    deletion.setCompanyId(companyId);
                    deletion.setDeleteType(DeleteType.BY_MESSAGE.getCode());
                    deletion.setChatType(chatType.getCode());
                    deletion.setChatId(chatId);
                    deletion.setUserId(userId);
                    deletion.setDeleteTime(new Date());
                    return deletion;
                }).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(newDeletions)) {
            this.saveBatch(newDeletions);
        }
    }

    @Transactional
    @Override
    public void deleteByChat(Long companyId, ChatType chatType, Long chatId, Long maxMessageId) {
        Long userId = SessionContext.getSession().getUserId();
        // 清理该会话之前删除的消息记录(整个会话都删了，之前的删除记录没作用了)
        LambdaQueryWrapper<MessageDeletion> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MessageDeletion::getUserId, userId);
        wrapper.eq(MessageDeletion::getCompanyId, companyId);
        wrapper.eq(MessageDeletion::getChatId, chatId);
        wrapper.eq(MessageDeletion::getChatType, chatType.getCode());
        wrapper.le(MessageDeletion::getMessageId, maxMessageId);
        this.remove(wrapper);
        // 存储新的删除记录
        MessageDeletion deletion = new MessageDeletion();
        deletion.setMessageId(maxMessageId);
        deletion.setCompanyId(companyId);
        deletion.setDeleteType(DeleteType.BY_CHAT.getCode());
        deletion.setChatType(chatType.getCode());
        deletion.setChatId(chatId);
        deletion.setUserId(userId);
        deletion.setDeleteTime(new Date());
        this.save(deletion);
    }

    @Override
    public List<MessageDeletion> findByChatType(Long companyId, ChatType chatType, Date minDate) {
        Long userId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<MessageDeletion> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MessageDeletion::getUserId, userId);
        wrapper.eq(MessageDeletion::getCompanyId, companyId);
        wrapper.eq(MessageDeletion::getChatType, chatType.getCode());
        wrapper.ge(MessageDeletion::getDeleteTime, minDate);
        return this.list(wrapper);
    }


}
