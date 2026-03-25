package com.bx.implatform.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.imclient.IMClient;
import com.bx.imcommon.contant.ChatConstant;
import com.bx.imcommon.model.IMGroupMessage;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.imcommon.util.CommaTextUtils;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.dto.*;
import com.bx.implatform.entity.*;
import com.bx.implatform.enums.ChatType;
import com.bx.implatform.enums.DeleteType;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.GroupMessageMapper;
import com.bx.implatform.service.GroupMemberService;
import com.bx.implatform.service.GroupMessageCompanyService;
import com.bx.implatform.service.GroupService;
import com.bx.implatform.service.MessageDeletionCompanyService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.util.SensitiveFilterUtil;
import com.bx.implatform.vo.GroupMessageVO;
import com.bx.implatform.vo.QuoteMessageVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageCompanyServiceImpl extends ServiceImpl<GroupMessageMapper, GroupMessage>
    implements GroupMessageCompanyService {
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final MessageDeletionCompanyService messageDeletionService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final IMClient imClient;
    private final SensitiveFilterUtil sensitiveFilterUtil;
    private final HttpServletRequest request;

    @Override
    public GroupMessageVO sendMessage(GroupMessageDTO dto) {
        validMessage(dto);
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        Group group = groupService.getAndCheckById(dto.getGroupId(), companyId);
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getGroupId(), userId,
                GroupMember::getId, GroupMember::getUserNickName, GroupMember::getRemarkNickName,
                GroupMember::getIsManager, GroupMember::getIsMuted, GroupMember::getQuit);
        boolean isOwner = userId.equals(group.getOwnerId());
        if (group.getIsAllMuted() && !group.getOwnerId().equals(userId) && !member.getIsManager()) {
            throw new GlobalException("群主开启了全员禁言模式,无法发送消息");
        }
        // 是否在群聊里面
        if (Objects.isNull(member) || member.getQuit()) {
            throw new GlobalException("您已不在群聊里面，无法发送消息");
        }
        if (member.getIsMuted()) {
            throw new GlobalException("您已被禁言，无法发送消息");
        }
        // 名片消息权限校验
        if (dto.getType().equals(MessageType.GROUP_CARD.code())) {
            if (!group.getIsAllowShareCard() && !isOwner && !member.getIsManager()) {
                throw new GlobalException("本群禁止普通成员分享名片");
            }
        }
        // 群聊成员列表
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(group.getId());
        if (dto.getReceipt() && userIds.size() > Constant.MAX_NORMAL_GROUP_MEMBER) {
            // 大群的回执消息过于消耗资源，不允许发送
            throw new GlobalException(String.format("当前群聊大于%s人,不支持发送回执消息", Constant.MAX_NORMAL_GROUP_MEMBER));
        }
        // 不用发给自己
        userIds = userIds.stream().filter(id -> !userId.equals(id)).collect(Collectors.toList());
        // 保存消息
        GroupMessage msg = BeanUtils.copyProperties(dto, GroupMessage.class);
        msg.setCompanyId(companyId);
        msg.setSendId(userId);
        msg.setSendTime(new Date());
        msg.setSendNickName(member.getShowNickName());
        msg.setAtUserIds(CommaTextUtils.asText(dto.getAtUserIds()));
        msg.setStatus(MessageStatus.PENDING.code());
        // 过滤内容中的敏感词
        if (MessageType.TEXT.code().equals(dto.getType())) {
            msg.setContent(sensitiveFilterUtil.filter(dto.getContent()));
        }
        this.save(msg);
        // 群发
        GroupMessageVO msgInfo = BeanUtils.copyProperties(msg, GroupMessageVO.class);
        // 填充引用消息
        if (!Objects.isNull(dto.getQuoteMessageId())) {
            GroupMessage quoteMessage = this.lambdaQuery()
                    .eq(GroupMessage::getId, dto.getQuoteMessageId())
                    .eq(GroupMessage::getCompanyId, companyId)
                    .one();
            msgInfo.setQuoteMessage(BeanUtils.copyProperties(quoteMessage, QuoteMessageVO.class));
            // 防止显示已撤回的内容
            if (quoteMessage.getStatus().equals(MessageStatus.RECALL.code())) {
                msgInfo.getQuoteMessage().setContent("引用内容已撤回");
            }
        }
        msgInfo.setAtUserIds(dto.getAtUserIds());
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setRecvIds(userIds);
        sendMessage.setData(msgInfo);
        imClient.sendGroupMessage(sendMessage);
        String ip = JakartaServletUtil.getClientIP(request);
        log.debug("发送群聊消息，ip:{},发送id:{},群聊id:{},内容:{}", ip, userId, dto.getGroupId(), dto.getContent());
        return msgInfo;
    }

    @Transactional
    @Override
    public GroupMessageVO recallMessage(Long id) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        GroupMessage msg = this.lambdaQuery()
                .select(GroupMessage::getId, GroupMessage::getGroupId, GroupMessage::getSendId, GroupMessage::getSendTime)
                .eq(GroupMessage::getId, id)
                .eq(GroupMessage::getCompanyId, companyId)
                .one();
        if (Objects.isNull(msg)) {
            throw new GlobalException("消息不存在");
        }
        Long groupId = msg.getGroupId();
        // 判断是否在群里
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, userId);
        if (Objects.isNull(member) || Boolean.TRUE.equals(member.getQuit())) {
            throw new GlobalException("您已不在群聊里面，无法撤回消息");
        }
        Group group = groupService.lambdaQuery()
                .select(Group::getId, Group::getOwnerId)
                .eq(Group::getId, groupId)
                .one();
        boolean isSender = msg.getSendId().equals(userId);
        boolean isOwner = group.getOwnerId().equals(userId);
        if (!isSender && !isOwner && !member.getIsManager()) {
            throw new GlobalException("您没有权限");
        }
        long time = System.currentTimeMillis() - msg.getSendTime().getTime();
        if (isSender && time > ChatConstant.ALLOW_RECALL_SECOND * 1000) {
            throw new GlobalException("消息已发送超过10分钟，无法撤回");
        }
        // 修改数据库
        this.lambdaUpdate()
            .eq(GroupMessage::getId, id)
            .eq(GroupMessage::getCompanyId, companyId)
            .set(GroupMessage::getStatus, MessageStatus.RECALL.code())
            .update();
        // 生成一条撤回消息
        GroupMessage recallMsg = new GroupMessage();
        recallMsg.setCompanyId(companyId);
        recallMsg.setStatus(MessageStatus.PENDING.code());
        recallMsg.setType(MessageType.RECALL.code());
        recallMsg.setGroupId(groupId);
        recallMsg.setSendId(userId);
        recallMsg.setSendNickName(member.getShowNickName());
        recallMsg.setContent(id.toString());
        recallMsg.setSendTime(new Date());
        this.save(recallMsg);
        // 群发
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        GroupMessageVO msgInfo = BeanUtils.copyProperties(recallMsg, GroupMessageVO.class);
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setRecvIds(userIds);
        sendMessage.setData(msgInfo);
        imClient.sendGroupMessage(sendMessage);
        log.debug("撤回群聊消息，发送id:{},群聊id:{}", userId, groupId);
        return msgInfo;
    }

    @Override
    public List<GroupMessageVO> loadOffineMessage(Long minId) {
        long time = System.currentTimeMillis();
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        List<GroupMessage> messages = new ArrayList<>();
        // 查询用户加入的群组
        List<GroupMember> members = groupMemberService.findByUserId(userId, GroupMember::getId, GroupMember::getCreateTime);
        Set<Long> groupIds = members.stream().map(GroupMember::getGroupId).collect(Collectors.toSet());
        // 只能拉取最近30天的消息
        Date minDate = DateUtils.addDays(new Date(), Math.toIntExact(-Constant.MAX_OFFLINE_MESSAGE_DAYS));
        if(!groupIds.isEmpty()){
            LambdaQueryWrapper<GroupMessage> wrapper = Wrappers.lambdaQuery();
            wrapper.gt(GroupMessage::getId, minId);
            wrapper.gt(GroupMessage::getCompanyId, companyId);
            wrapper.gt(GroupMessage::getSendTime, minDate);
            wrapper.in(GroupMessage::getGroupId, groupIds);
            wrapper.orderByDesc(GroupMessage::getId);
            wrapper.last("limit 50000");
            messages = this.list(wrapper);
        }
        // 查询退群前的消息
        Date minQuitTime = minDate;
        if (minId > 0) {
            // 如果某个群的退群时间大于起始消息的发送时间，那消息是不用推送的，过滤掉
            GroupMessage message = this.getById(minId);
            if (!Objects.isNull(message) && message.getSendTime().compareTo(minDate) > 0) {
                minQuitTime = message.getSendTime();
            }
        }
        List<GroupMessage> quitMessages = Collections.synchronizedList(new ArrayList<>());
        List<GroupMember> quitMembers = groupMemberService.findQuitMembers(userId, minQuitTime, GroupMember::getId, GroupMember::getCreateTime);
        quitMembers.parallelStream().forEach(quitMember -> {
            LambdaQueryWrapper<GroupMessage> quitWrapper = Wrappers.lambdaQuery();
            quitWrapper.gt(GroupMessage::getId, minId);
            quitWrapper.eq(GroupMessage::getCompanyId, companyId);
            quitWrapper.between(GroupMessage::getSendTime, minDate, quitMember.getQuitTime());
            quitWrapper.eq(GroupMessage::getGroupId, quitMember.getGroupId());
            quitWrapper.orderByDesc(GroupMessage::getId);
            quitWrapper.last("limit 1000");
            List<GroupMessage> groupMessages = this.list(quitWrapper);
            quitMessages.addAll(groupMessages);
        });
        messages.addAll(quitMessages);
        members.addAll(quitMembers);
        // 转成map方便提取
        Map<Long, Date> groupMemberMap = CollStreamUtil.toMap(members, GroupMember::getGroupId, GroupMember::getCreateTime);
        // 通过群聊对消息进行分组
        Map<Long, List<GroupMessage>> messageGroupMap = messages.stream().collect(Collectors.groupingBy(GroupMessage::getGroupId));
        // 引用消息
        Map<Long, QuoteMessageVO> quoteMessageMap = batchLoadQuoteMessage(companyId, messages);
        // 已经删除的消息
        List<MessageDeletion> deletions = messageDeletionService.findByChatType(companyId, ChatType.GROUP, minDate);
        List<GroupMessageVO> vos = new LinkedList<>();
        for (Map.Entry<Long, List<GroupMessage>> entry : messageGroupMap.entrySet()) {
            Long groupId = entry.getKey();
            List<GroupMessage> groupMessages = entry.getValue();
            // 填充消息状态
            String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
            Object o = redisTemplate.opsForHash().get(key, userId.toString());
            long readedMaxId = Objects.isNull(o) ? -1 : Long.parseLong(o.toString());
            Map<Object, Object> maxIdMap = null;
            for (GroupMessage m : groupMessages) {
                // 排除加群之前的消息
                if (DateUtil.compare(groupMemberMap.get(m.getGroupId()), m.getSendTime()) > 0) {
                    continue;
                }
                // 排除不需要接收的消息
                List<String> recvIds = CommaTextUtils.asList(m.getRecvIds());
                if (!recvIds.isEmpty() && !recvIds.contains(userId.toString())) {
                    continue;
                }
                // 排除已经删除的消息
                if (isDeleteMessage(m, deletions)) {
                    continue;
                }
                // 组装vo
                GroupMessageVO vo = BeanUtils.copyProperties(m, GroupMessageVO.class);
                // 引用消息
                vo.setQuoteMessage(quoteMessageMap.get(m.getQuoteMessageId()));
                // 被@用户列表
                List<String> atIds = CommaTextUtils.asList(m.getAtUserIds());
                vo.setAtUserIds(atIds.stream().map(Long::parseLong).collect(Collectors.toList()));
                // 填充状态
                vo.setStatus(readedMaxId >= m.getId() ? MessageStatus.READED.code() : MessageStatus.PENDING.code());
                // 针对回执消息填充已读人数
                if (m.getReceipt()) {
                    if (Objects.isNull(maxIdMap)) {
                        maxIdMap = redisTemplate.opsForHash().entries(key);
                    }
                    int count = getReadedUserIds(maxIdMap, m.getId(), m.getSendId()).size();
                    vo.setReadedCount(count);
                }
                vos.add(vo);
            }
        }
        log.info("拉取离线群聊消息，用户id:{},数量:{},耗时:{},minId:{}", userId, vos.size(), System.currentTimeMillis() - time, minId);
        // 排序
        return vos.stream().sorted(Comparator.comparing(GroupMessageVO::getId)).collect(Collectors.toList());
    }

    @Override
    public void readedMessage(Long groupId) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        // 取出最后的消息id
        LambdaQueryWrapper<GroupMessage> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GroupMessage::getGroupId, groupId)
            .select(GroupMessage::getId)
            .eq(GroupMessage::getCompanyId, companyId)
            .orderByDesc(GroupMessage::getId)
            .last("limit 1");
        GroupMessage message = this.getOne(wrapper);
        if (Objects.isNull(message)) {
            return;
        }
        // 推送消息给自己的其他终端,同步清空会话列表中的未读数量
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setType(MessageType.READED.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setSendId(userId);
        msgInfo.setGroupId(groupId);
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setSendToSelf(true);
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        imClient.sendGroupMessage(sendMessage);
        // 已读消息key
        String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
        // 原来的已读消息位置
        Object maxReadedId = redisTemplate.opsForHash().get(key, userId.toString());
        // 记录已读消息位置
        redisTemplate.opsForHash().put(key, userId.toString(), message.getId());
        // 推送消息回执，刷新已读人数显示
        wrapper = Wrappers.lambdaQuery();
        wrapper.select(GroupMessage::getId, GroupMessage::getSendId);
        wrapper.gt(!Objects.isNull(maxReadedId), GroupMessage::getId, maxReadedId);
        wrapper.le(!Objects.isNull(maxReadedId), GroupMessage::getId, message.getId());
        wrapper.eq(GroupMessage::getCompanyId, companyId);
        wrapper.eq(GroupMessage::getGroupId, groupId);
        wrapper.ne(GroupMessage::getStatus, MessageStatus.RECALL.code());
        wrapper.eq(GroupMessage::getReceipt, true);
        List<GroupMessage> receiptMessages = this.list(wrapper);
        if (CollectionUtil.isNotEmpty(receiptMessages)) {
            List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
            Map<Object, Object> maxIdMap = redisTemplate.opsForHash().entries(key);
            for (GroupMessage receiptMessage : receiptMessages) {
                msgInfo = new GroupMessageVO();
                int readedCount = getReadedUserIds(maxIdMap, receiptMessage.getId(), receiptMessage.getSendId()).size();
                // 如果所有人都已读，记录回执消息完成标记
                if (readedCount >= userIds.size() - 1) {
                    msgInfo.setReceiptOk(true);
                    this.lambdaUpdate()
                        .eq(GroupMessage::getId, receiptMessage.getId())
                        .eq(GroupMessage::getCompanyId, companyId)
                        .set(GroupMessage::getReceiptOk, true)
                        .update();
                }
                msgInfo.setId(receiptMessage.getId());
                msgInfo.setGroupId(groupId);
                msgInfo.setReadedCount(readedCount);
                msgInfo.setType(MessageType.RECEIPT.code());
                sendMessage = new IMGroupMessage<>();
                sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
                sendMessage.setRecvIds(userIds);
                sendMessage.setData(msgInfo);
                sendMessage.setSendToSelf(false);
                sendMessage.setSendResult(false);
                imClient.sendGroupMessage(sendMessage);
            }
        }
    }

    @Override
    public List<Long> findReadedUsers(Long groupId, Long messageId) {
        UserSession session = SessionContext.getSession();
        GroupMessage message = this.lambdaQuery()
                .select(GroupMessage::getId, GroupMessage::getSendId)
                .eq(GroupMessage::getId, messageId)
                .one();
        if (Objects.isNull(message)) {
            throw new GlobalException("消息不存在");
        }
        // 是否在群聊里面
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId(), GroupMember::getId, GroupMember::getQuit);
        if (Objects.isNull(member) || member.getQuit()) {
            throw new GlobalException("您已不在群聊里面");
        }
        // 已读位置key
        String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
        // 一次获取所有用户的已读位置
        Map<Object, Object> maxIdMap = redisTemplate.opsForHash().entries(key);
        // 返回已读用户的id集合
        return getReadedUserIds(maxIdMap, message.getId(), message.getSendId());
    }

    @Override
    public void deleteMessage(MessageDeleteDTO dto) {
        messageDeletionService.deleteByMessage(SessionContext.getSession().getCompanyId(), ChatType.GROUP, dto.getChatId(), dto.getMessageIds());
    }

    @Override
    public void deleteChat(ChatDeleteDTO dto) {
        Long companyId = SessionContext.getSession().getCompanyId();
        // 获取会话中最后一条消息
        LambdaQueryWrapper<GroupMessage> wrapper = Wrappers.lambdaQuery();
        wrapper.select(GroupMessage::getId);
        wrapper.eq(GroupMessage::getGroupId, dto.getChatId());
        wrapper.eq(GroupMessage::getCompanyId, companyId);
        wrapper.orderByDesc(GroupMessage::getId);
        wrapper.last("limit 1");
        GroupMessage message = this.getOne(wrapper);
        if (Objects.isNull(message)) {
            return;
        }
        // 保存删除记录
        messageDeletionService.deleteByChat(companyId, ChatType.GROUP, dto.getChatId(), message.getId());
    }

    @Override
    public GroupMessageVO remove(GroupMessageRemoveDTO dto) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        Long groupId = dto.getGroupId();
        Group group = groupService.getAndCheckById(groupId, companyId);
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId(),
                GroupMember::getId, GroupMember::getUserNickName, GroupMember::getRemarkNickName, GroupMember::getQuit);
        if (Objects.isNull(member) || Boolean.TRUE.equals(member.getQuit())) {
            throw new GlobalException("您已不在群聊里面");
        }
        if (!group.getOwnerId().equals(userId) && !member.getIsManager()) {
            throw new GlobalException("您没有权限");
        }
        messageDeletionService.deleteByMessage(companyId, ChatType.GROUP, groupId, dto.getMessageIds());
        // 生成一条移除消息
        GroupMessage removeMsg = new GroupMessage();
        removeMsg.setCompanyId(companyId);
        removeMsg.setStatus(MessageStatus.PENDING.code());
        removeMsg.setType(MessageType.REMOVED.code());
        removeMsg.setGroupId(groupId);
        removeMsg.setSendId(userId);
        removeMsg.setSendNickName(member.getShowNickName());
        removeMsg.setContent(CommaTextUtils.asText(dto.getMessageIds()));
        removeMsg.setSendTime(new Date());
        this.save(removeMsg);
        // 群发
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        GroupMessageVO msgInfo = BeanUtils.copyProperties(removeMsg, GroupMessageVO.class);
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setRecvIds(userIds);
        sendMessage.setData(msgInfo);
        imClient.sendGroupMessage(sendMessage);
        log.info("移除群聊消息，用户id:{},群聊id:{},消息id:{}", userId, groupId, dto.getMessageIds());
        return msgInfo;
    }

    @Override
    public GroupMessageVO removeAll(GroupMessageRemoveAllDTO dto) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        Long companyId = session.getCompanyId();
        Long groupId = dto.getGroupId();
        Group group = groupService.getAndCheckById(groupId, companyId);
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, userId,
                GroupMember::getId, GroupMember::getUserNickName, GroupMember::getRemarkNickName, GroupMember::getQuit);
        if (Objects.isNull(member) || Boolean.TRUE.equals(member.getQuit())) {
            throw new GlobalException("您已不在群聊里面");
        }
        if (!group.getOwnerId().equals(userId) && !member.getIsManager()) {
            throw new GlobalException("您没有权限");
        }
        Long messageId = dto.getMessageId();
        messageDeletionService.deleteByChat(companyId, ChatType.GROUP, groupId, messageId);
        // 生成一条移除消息
        GroupMessage removeMsg = new GroupMessage();
        removeMsg.setCompanyId(companyId);
        removeMsg.setStatus(MessageStatus.PENDING.code());
        removeMsg.setType(MessageType.REMOVED_ALL.code());
        removeMsg.setGroupId(groupId);
        removeMsg.setSendId(userId);
        removeMsg.setSendNickName(member.getShowNickName());
        removeMsg.setContent(Convert.toStr(messageId));
        removeMsg.setSendTime(new Date());
        this.save(removeMsg);
        // 群发
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        GroupMessageVO msgInfo = BeanUtils.copyProperties(removeMsg, GroupMessageVO.class);
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, session.getTerminal()));
        sendMessage.setRecvIds(userIds);
        sendMessage.setData(msgInfo);
        imClient.sendGroupMessage(sendMessage);
        log.info("移除全部群聊消息，发送id:{},群聊id:{},消息id:{}", userId, groupId, messageId);
        return msgInfo;
    }

    private List<Long> getReadedUserIds(Map<Object, Object> maxIdMap, Long messageId, Long sendId) {
        List<Long> userIds = new LinkedList<>();
        maxIdMap.forEach((k, v) -> {
            Long userId = Long.valueOf(k.toString());
            long maxId = Long.parseLong(v.toString());
            // 发送者不计入已读人数
            if (!sendId.equals(userId) && maxId >= messageId) {
                userIds.add(userId);
            }
        });
        return userIds;
    }

    private void sendLoadingMessage(Boolean isLoading, UserSession session) {
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setType(MessageType.LOADING.code());
        msgInfo.setContent(isLoading.toString());
        IMGroupMessage sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvIds(Collections.singletonList(session.getUserId()));
        sendMessage.setRecvTerminals(Collections.singletonList(session.getTerminal()));
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(false);
        sendMessage.setSendResult(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private Map<Long, QuoteMessageVO> batchLoadQuoteMessage(Long companyId, List<GroupMessage> messages) {
        // 提取列表中所有引用消息
        List<Long> ids = messages.stream()
                .map(GroupMessage::getQuoteMessageId)
                .filter(quoteMessageId -> !Objects.isNull(quoteMessageId))
                .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(ids)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<GroupMessage> wrapper = Wrappers.lambdaQuery();
        wrapper.in(GroupMessage::getId, ids);
        wrapper.in(GroupMessage::getCompanyId, companyId);
        List<GroupMessage> quoteMessages = this.list(wrapper);
        // 转为vo
        return quoteMessages.stream().collect(Collectors.toMap(GroupMessage::getId, m -> BeanUtils.copyProperties(m, QuoteMessageVO.class)));
    }

    private void validMessage(GroupMessageDTO dto) {
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

    private Boolean isDeleteMessage(GroupMessage message, List<MessageDeletion> deletions) {
        return deletions.stream().anyMatch(deletion -> {
            if (!message.getGroupId().equals(deletion.getChatId())) {
                return false;
            }
            if (DeleteType.BY_CHAT.getCode().equals(deletion.getDeleteType())) {
                return deletion.getMessageId() >= message.getId();
            }
            return message.getId().equals(deletion.getMessageId());
        });
    }

}
