package com.bx.implatform.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.imclient.IMClient;
import com.bx.imcommon.contant.ChatConstant;
import com.bx.imcommon.model.IMPrivateMessage;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.dto.ChatDeleteDTO;
import com.bx.implatform.dto.MessageDeleteDTO;
import com.bx.implatform.dto.PrivateMessageDTO;
import com.bx.implatform.entity.MessageDeletion;
import com.bx.implatform.entity.PrivateMessage;
import com.bx.implatform.enums.ChatType;
import com.bx.implatform.enums.DeleteType;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.PrivateMessageMapper;
import com.bx.implatform.service.FriendService;
import com.bx.implatform.service.MessageDeletionCompanyService;
import com.bx.implatform.service.PrivateMessageCompanyService;
import com.bx.implatform.service.UserBlacklistService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.util.SensitiveFilterUtil;
import com.bx.implatform.vo.PrivateMessageVO;
import com.bx.implatform.vo.QuoteMessageVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateMessageCompanyServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage>
    implements PrivateMessageCompanyService {

    private final FriendService friendService;
    private final UserBlacklistService userBlacklistService;
    private final MessageDeletionCompanyService messageDeletionService;
    private final IMClient imClient;
    private final SensitiveFilterUtil sensitiveFilterUtil;
    private final HttpServletRequest request;

    @Override
    public PrivateMessageVO sendMessage(PrivateMessageDTO dto) {
        validMessage(dto);
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        if (!friendService.isFriend(userId, dto.getRecvId())) {
            throw new GlobalException("您已不是对方好友，无法发送消息");
        }
        if (userBlacklistService.isInBlacklist(dto.getRecvId(), userId)) {
            throw new GlobalException("对方已将您拉入黑名单，无法发送消息");
        }
        // 保存消息
        PrivateMessage msg = BeanUtils.copyProperties(dto, PrivateMessage.class);
        msg.setSendId(userId);
        msg.setCompanyId(companyId);
        msg.setStatus(MessageStatus.PENDING.code());
        msg.setSendTime(new Date());
        // 过滤内容中的敏感词
        if (MessageType.TEXT.code().equals(dto.getType())) {
            msg.setContent(sensitiveFilterUtil.filter(dto.getContent()));
        }
        this.save(msg);
        // 推送消息
        PrivateMessageVO msgInfo = BeanUtils.copyProperties(msg, PrivateMessageVO.class);
        // 填充引用消息
        if (!Objects.isNull(dto.getQuoteMessageId())) {
            PrivateMessage quoteMessage = this.lambdaQuery()
                    .eq(PrivateMessage::getId, dto.getQuoteMessageId())
                    .eq(PrivateMessage::getCompanyId, companyId)
                    .one();
            msgInfo.setQuoteMessage(BeanUtils.copyProperties(quoteMessage, QuoteMessageVO.class));
            // 防止显示已撤回的内容
            if (quoteMessage.getStatus().equals(MessageStatus.RECALL.code())) {
                msgInfo.getQuoteMessage().setContent("引用内容已撤回");
            }
        }
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setRecvId(msgInfo.getRecvId());
        sendMessage.setSendToSelf(true);
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(true);
        imClient.sendPrivateMessage(sendMessage);
        String ip = JakartaServletUtil.getClientIP(request);
        log.info("发送私聊消息，ip:{},sendId:{},recvId:{},content:{}", ip, userId, dto.getRecvId(), dto.getContent());
        return msgInfo;
    }

    @Transactional
    @Override
    public PrivateMessageVO recallMessage(Long id) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        PrivateMessage msg = this.lambdaQuery()
                .select(PrivateMessage::getId, PrivateMessage::getSendId, PrivateMessage::getRecvId, PrivateMessage::getSendTime)
                .eq(PrivateMessage::getId, id)
                .eq(PrivateMessage::getCompanyId, companyId)
                .one();
        if (Objects.isNull(msg)) {
            throw new GlobalException("消息不存在");
        }
        if (!msg.getSendId().equals(companyId)) {
            throw new GlobalException("这条消息不是由您发送,无法撤回");
        }
        if (System.currentTimeMillis() - msg.getSendTime().getTime() > ChatConstant.ALLOW_RECALL_SECOND * 1000) {
            throw new GlobalException("消息已发送超过5分钟，无法撤回");
        }
        // 修改消息状态
        this.lambdaUpdate()
                .eq(PrivateMessage::getId, id)
                .eq(PrivateMessage::getCompanyId, companyId)
                .set(PrivateMessage::getStatus, MessageStatus.RECALL.code())
                .update();
        Long recvId = msg.getRecvId();
        // 生成一条撤回消息
        PrivateMessage recallMsg = new PrivateMessage();
        recallMsg.setCompanyId(session.getCompanyId());
        recallMsg.setSendId(userId);
        recallMsg.setStatus(MessageStatus.PENDING.code());
        recallMsg.setSendTime(new Date());
        recallMsg.setRecvId(recvId);
        recallMsg.setType(MessageType.RECALL.code());
        recallMsg.setContent(id.toString());
        this.save(recallMsg);
        // 推送消息
        PrivateMessageVO msgInfo = BeanUtils.copyProperties(recallMsg, PrivateMessageVO.class);
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setRecvId(recvId);
        sendMessage.setData(msgInfo);
        imClient.sendPrivateMessage(sendMessage);
        log.info("撤回私聊消息，sendId:{},recvId:{}", userId, recvId);
        return msgInfo;
    }

    @Override
    public List<PrivateMessageVO> loadOfflineMessage(Long minId) {
        long time = System.currentTimeMillis();
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        // 获取当前用户的消息
        LambdaQueryWrapper<PrivateMessage> wrapper = Wrappers.lambdaQuery();
        // 只能拉取最近1个月的消息
        Date minDate = DateUtils.addDays(new Date(), Math.toIntExact(-Constant.MAX_OFFLINE_MESSAGE_DAYS));
        wrapper.gt(PrivateMessage::getId, minId);
        wrapper.eq(PrivateMessage::getCompanyId, companyId);
        wrapper.ge(PrivateMessage::getSendTime, minDate);
        wrapper.and(wp -> wp
                .eq(PrivateMessage::getSendId, userId)
                .or()
                .eq(PrivateMessage::getRecvId, userId));
        wrapper.orderByAsc(PrivateMessage::getId);
        List<PrivateMessage> messages = this.list(wrapper);
        // 排除已经删除的消息
        List<MessageDeletion> deletions = messageDeletionService.findByChatType(companyId, ChatType.PRIVATE, minDate);
        messages = messages.stream().filter(m -> !isDeleteMessage(m, deletions)).collect(Collectors.toList());
        // 更新消息为送达状态
        List<Long> messageIds = messages.stream()
                .filter(m -> m.getRecvId().equals(userId) && m.getStatus().equals(MessageStatus.PENDING.code()))
                .map(PrivateMessage::getId)
                .collect(Collectors.toList());
        if (!messageIds.isEmpty()) {
            LambdaUpdateWrapper<PrivateMessage> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.in(PrivateMessage::getId, messageIds);
            updateWrapper.eq(PrivateMessage::getCompanyId, companyId);
            updateWrapper.set(PrivateMessage::getStatus, MessageStatus.DELIVERED.code());
            update(updateWrapper);
        }
        // 提取所有引用消息
        Map<Long, QuoteMessageVO> quoteMessageMap = batchLoadQuoteMessage(companyId, messages);
        // 转换vo
        List<PrivateMessageVO> vos = messages.stream().map(m -> {
            PrivateMessageVO vo = BeanUtils.copyProperties(m, PrivateMessageVO.class);
            vo.setQuoteMessage(quoteMessageMap.get(m.getQuoteMessageId()));
            return vo;
        }).collect(Collectors.toList());
        log.info("拉取离线私聊消息，用户id:{},数量:{},耗时:{},minId:{}", userId, vos.size(), System.currentTimeMillis() - time, minId);
        return vos;
    }

    @Override
    public void readedMessage(Long friendId) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        // 推送消息给自己，清空会话列表上的已读数量
        PrivateMessageVO msgInfo = new PrivateMessageVO();
        msgInfo.setType(MessageType.READED.code());
        msgInfo.setSendId(userId);
        msgInfo.setRecvId(friendId);
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setData(msgInfo);
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setSendToSelf(true);
        sendMessage.setSendResult(false);
        imClient.sendPrivateMessage(sendMessage);
        // 推送回执消息给对方，更新已读状态
        msgInfo = new PrivateMessageVO();
        msgInfo.setType(MessageType.RECEIPT.code());
        msgInfo.setSendId(userId);
        msgInfo.setRecvId(friendId);
        sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setRecvId(friendId);
        sendMessage.setSendToSelf(false);
        sendMessage.setSendResult(false);
        sendMessage.setData(msgInfo);
        imClient.sendPrivateMessage(sendMessage);
        try {
            log.info("消息已读开始执行sql，接收方id:{},发送方id:{}", userId, friendId);
            // 修改消息状态为已读
            LambdaUpdateWrapper<PrivateMessage> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(PrivateMessage::getCompanyId, session.getCompanyId());
            updateWrapper.eq(PrivateMessage::getSendId, friendId);
            updateWrapper.eq(PrivateMessage::getRecvId, userId);
            updateWrapper.eq(PrivateMessage::getStatus, MessageStatus.DELIVERED.code());
            updateWrapper.set(PrivateMessage::getStatus, MessageStatus.READED.code());
            this.update(updateWrapper);
        } catch (Exception e) {
            log.error("消息状态修改失败,userId:{},friendId:{}", userId, friendId, e);
        }
        log.info("消息已读，接收方id:{},发送方id:{}", userId, friendId);
    }

    @Override
    public Long getMaxReadedId(Long friendId) {
        UserSession session = SessionContext.getSession();
        LambdaQueryWrapper<PrivateMessage> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PrivateMessage::getCompanyId, session.getCompanyId());
        wrapper.eq(PrivateMessage::getSendId, session.getUserId());
        wrapper.eq(PrivateMessage::getRecvId, friendId);
        wrapper.eq(PrivateMessage::getStatus, MessageStatus.READED.code());
        wrapper.orderByDesc(PrivateMessage::getId);
        wrapper.select(PrivateMessage::getId).last("limit 1");
        PrivateMessage message = this.getOne(wrapper);
        if (Objects.isNull(message)) {
            return -1L;
        }
        return message.getId();
    }

    @Override
    public void deleteMessage(MessageDeleteDTO dto) {
        messageDeletionService.deleteByMessage(SessionContext.getSession().getCompanyId(), ChatType.PRIVATE, dto.getChatId(), dto.getMessageIds());
    }

    @Override
    public void deleteChat(ChatDeleteDTO dto) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        // 查询会话最大消息id
        LambdaQueryWrapper<PrivateMessage> wrapper = Wrappers.lambdaQuery();
        wrapper.select(PrivateMessage::getId);
        wrapper.eq(PrivateMessage::getCompanyId, companyId)
                .and(wrap -> wrap
                        .and(wp -> wp.eq(PrivateMessage::getSendId, userId).eq(PrivateMessage::getRecvId, dto.getChatId()))
                        .or(wp -> wp.eq(PrivateMessage::getRecvId, userId).eq(PrivateMessage::getSendId, dto.getChatId()))
                );
        wrapper.orderByDesc(PrivateMessage::getId);
        wrapper.last("limit 1");
        PrivateMessage message = this.getOne(wrapper);
        if (Objects.isNull(message)) {
            return;
        }
        messageDeletionService.deleteByChat(companyId, ChatType.PRIVATE, dto.getChatId(), message.getId());
    }

    private void sendLoadingMessage(Boolean isLoading, UserSession session) {
        PrivateMessageVO msgInfo = new PrivateMessageVO();
        msgInfo.setType(MessageType.LOADING.code());
        msgInfo.setContent(isLoading.toString());
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvId(session.getUserId());
        sendMessage.setRecvTerminals(List.of(session.getTerminal()));
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setSendResult(false);
        imClient.sendPrivateMessage(sendMessage);
    }

    private Map<Long, QuoteMessageVO> batchLoadQuoteMessage(Long companyId, List<PrivateMessage> messages) {
        // 提取列表中所有引用消息
        List<Long> ids = messages.stream()
                .map(PrivateMessage::getQuoteMessageId)
                .filter(quoteMessageId -> !Objects.isNull(quoteMessageId)).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(ids)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<PrivateMessage> wrapper = Wrappers.lambdaQuery();
        wrapper.in(PrivateMessage::getId, ids);
        wrapper.eq(PrivateMessage::getCompanyId, companyId);
        List<PrivateMessage> quoteMessages = this.list(wrapper);
        // 转为vo
        return quoteMessages.stream().collect(Collectors.toMap(PrivateMessage::getId, m -> BeanUtils.copyProperties(m, QuoteMessageVO.class)));
    }

    private void validMessage(PrivateMessageDTO dto) {
        // 只允许用户发送普通消息
        if (dto.getType() < MessageType.TEXT.code() || dto.getType() > MessageType.MERGE_FORWARD.code()) {
            throw new GlobalException(String.format("消息类型不合法"));
        }
        // 文字消息-长度校验
        if (MessageType.TEXT.code().equals(dto.getType()) && dto.getContent().length() > Constant.MAX_MESSAGE_LENGTH) {
            throw new GlobalException(String.format("消息长度不能大于%d个字符", Constant.MAX_MESSAGE_LENGTH));
        }
        try {
            // 非文字消息-保证数据格式是json,防止前端报错
            if (!MessageType.TEXT.code().equals(dto.getType())) {
                JSON.parse(dto.getContent());
            }
        } catch (Exception e) {
            throw new GlobalException("消息格式异常");
        }
    }

    private Boolean isDeleteMessage(PrivateMessage message, List<MessageDeletion> deletions) {
        return deletions.stream().anyMatch(deletion -> {
            if (!message.getSendId().equals(deletion.getChatId()) && !message.getRecvId().equals(deletion.getChatId())) {
                return false;
            }
            if (DeleteType.BY_CHAT.getCode().equals(deletion.getDeleteType())) {
                return deletion.getMessageId() >= message.getId();
            }
            return message.getId().equals(deletion.getMessageId());
        });
    }
}
