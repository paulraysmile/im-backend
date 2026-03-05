package com.bx.implatform.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.imclient.IMClient;
import com.bx.imcommon.model.IMGroupMessage;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.imcommon.util.CommaTextUtils;
import com.bx.implatform.annotation.RedisLock;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.dto.*;
import com.bx.implatform.entity.*;
import com.bx.implatform.enums.BizTokenType;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.GroupMapper;
import com.bx.implatform.mapper.GroupMessageMapper;
import com.bx.implatform.mapper.UserMapper;
import com.bx.implatform.service.FriendService;
import com.bx.implatform.service.GroupMemberService;
import com.bx.implatform.service.GroupService;
import com.bx.implatform.service.WebrtcGroupService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = RedisKey.IM_CACHE_GROUP)
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {
    private final UserMapper userMapper;
    private final GroupMemberService groupMemberService;
    private final GroupMessageMapper groupMessageMapper;
    private final FriendService friendsService;
    private final IMClient imClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FriendService friendService;
    @Lazy
    @Autowired
    private WebrtcGroupService webrtcGroupService;

    @Override
    public GroupVO createGroup(GroupVO vo) {
        UserSession session = SessionContext.getSession();
        User user = userMapper.selectById(session.getUserId());
        // 保存群组数据
        Group group = BeanUtils.copyProperties(vo, Group.class);
        group.setOwnerId(user.getId());
        this.save(group);
        // 把群主加入群
        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(user.getId());
        member.setUserNickName(user.getNickName());
        member.setHeadImage(user.getHeadImageThumb());
        member.setRemarkNickName(vo.getRemarkNickName());
        member.setRemarkGroupName(vo.getRemarkGroupName());
        member.setCompanyName(user.getCompanyName());
        groupMemberService.save(member);
        GroupVO groupVo = findById(group.getId());
        // 推送同步消息给自己的其他终端
        sendAddGroupMessage(groupVo, List.of(session.getUserId()));
        // 返回
        log.info("创建群聊，群聊id:{},群聊名称:{}", group.getId(), group.getName());
        return groupVo;
    }

    @CacheEvict(key = "#vo.getId()")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public GroupVO modifyGroup(GroupVO vo) {
        UserSession session = SessionContext.getSession();
        // 校验是不是群主，只有群主能改信息
        Group group = this.getAndCheckById(vo.getId());
        // 更新成员信息
        GroupMember member = groupMemberService.findByGroupAndUserId(vo.getId(), session.getUserId());
        if (Objects.isNull(member) || member.getQuit()) {
            throw new GlobalException("您不是群聊的成员");
        }
        member.setRemarkNickName(vo.getRemarkNickName());
        member.setRemarkGroupName(vo.getRemarkGroupName());
        member.setVersion(groupMemberService.getNextVersion(vo.getId()));
        groupMemberService.updateById(member);
        // 群主和管理员有权修改群基本信息
        if (group.getOwnerId().equals(session.getUserId()) || member.getIsManager()) {
            // 禁言模式发生修改，推送提示语
            if (!Objects.isNull(vo.getIsAllMuted()) && !group.getIsAllMuted().equals(vo.getIsAllMuted())) {
                List<Long> userIds = groupMemberService.findUserIdsByGroupId(vo.getId());
                sendMutedTip(vo.getId(), member, userIds, vo.getIsAllMuted());
                sendSyncMessage(vo.getId(), userIds, MessageType.GROUP_ALL_MUTED, vo.getIsAllMuted());
            }
            group = BeanUtils.copyProperties(vo, Group.class);
            this.updateById(group);
        }
        log.info("修改群聊，群聊id:{},群聊名称:{}", group.getId(), group.getName());
        return convert(group, member);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "#groupId")
    @Override
    public void deleteGroup(Long groupId) {
        UserSession session = SessionContext.getSession();
        Group group = this.getById(groupId);
        if (!group.getOwnerId().equals(session.getUserId())) {
            throw new GlobalException("只有群主才有权限解除群聊");
        }
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        // 群聊用户id
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        // 逻辑删除群数据
        group.setDissolve(true);
        this.updateById(group);
        // 删除成员数据
        groupMemberService.removeByGroupId(groupId);
        // 清理已读缓存
        String key = StrUtil.join(":", RedisKey.IM_GROUP_READED_POSITION, groupId);
        redisTemplate.delete(key);
        // 清理成员版本号
        key = StrUtil.join(":", RedisKey.IM_GROUP_MEMBER_MAX_VERSION, groupId);
        redisTemplate.delete(key);
        // 推送解散群聊提示
        String content = String.format("%s解散了群聊", formatUserMark(member));
        this.sendTipMessage(groupId, userIds, content, true);
        // 推送同步消息所有用户
        this.sendDelGroupMessage(groupId, userIds);
        log.info("删除群聊，群聊id:{},群聊名称:{}", group.getId(), group.getName());
    }

    @Override
    public void quitGroup(Long groupId) {
        Long userId = SessionContext.getSession().getUserId();
        Group group = this.getById(groupId);
        if (group.getOwnerId().equals(userId)) {
            throw new GlobalException("您是群主，不可退出群聊");
        }
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, userId);
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        // 删除群聊成员
        groupMemberService.removeByGroupAndUserId(groupId, userId);
        // 推送退出提示给其他成员
        String content = String.format("%s退出了群聊", formatUserMark(member));
        this.sendTipMessage(groupId, userIds, content, true);
        // 推送同步消息给其他终端
        this.sendDelGroupMessage(groupId, List.of(userId));
        log.info("退出群聊，群聊id:{},群聊名称:{},用户id:{}", group.getId(), group.getName(), userId);
    }

    @Override
    public void removeGroupMembers(GroupMemberRemoveDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = this.getAndCheckById(dto.getGroupId());
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getGroupId(), session.getUserId());
        boolean isOwner = group.getOwnerId().equals(session.getUserId());
        if (!isOwner && !member.getIsManager()) {
            throw new GlobalException("您没有权限");
        }
        if (dto.getUserIds().contains(group.getOwnerId())) {
            throw new GlobalException("不允许移除群主");
        }
        if (dto.getUserIds().contains(session.getUserId())) {
            throw new GlobalException("不允许移除自己");
        }
        List<GroupMember> members = groupMemberService.findByGroupAndUserIds(dto.getGroupId(), dto.getUserIds());
        boolean hasManager = members.stream().anyMatch(GroupMember::getIsManager);
        if (!isOwner && hasManager) {
            throw new GlobalException("您没有移除管理员的权限");
        }
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(dto.getGroupId());
        // 删除群聊成员
        groupMemberService.removeByGroupAndUserIds(dto.getGroupId(), dto.getUserIds());
        // 推送踢出群聊提示
        String content = String.format("%s将%s移出了群聊", formatUserMark(member), formatUserMarks(members));
        this.sendTipMessage(dto.getGroupId(), userIds, content, true);
        // 推送同步消息
        this.sendDelGroupMessage(dto.getGroupId(), dto.getUserIds());
        log.info("踢出群聊，群聊id:{},群聊名称:{},用户id:{}", group.getId(), group.getName(), dto.getUserIds());
    }

    @Override
    public GroupVO findById(Long groupId) {
        UserSession session = SessionContext.getSession();
        Group group = super.getById(groupId);
        if (Objects.isNull(group)) {
            throw new GlobalException("群聊不存在");
        }
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        GroupVO vo = convert(group, member);
        // 填充群置顶消息
        if (!Objects.isNull(group.getTopMessageId()) && !Objects.isNull(member) && member.getIsTopMessage()) {
            GroupMessage message = groupMessageMapper.selectById(group.getTopMessageId());
            if (!Objects.isNull(message)) {
                vo.setTopMessage(BeanUtils.copyProperties(message, GroupMessageVO.class));
            }
        }
        // 通话信息
        WebrtcGroupInfoVO rtcInfo = webrtcGroupService.info(groupId);
        vo.setRtcInfo(rtcInfo);
        return vo;
    }

    @Cacheable(key = "#groupId")
    @Override
    public Group getAndCheckById(Long groupId) {
        Group group = super.getById(groupId);
        if (Objects.isNull(group)) {
            throw new GlobalException("群组不存在");
        }
        if (group.getDissolve()) {
            throw new GlobalException("群组'" + group.getName() + "'已解散");
        }
        if (group.getIsBanned()) {
            throw new GlobalException("群组'" + group.getName() + "'已被封禁,原因:" + group.getReason());
        }
        return group;
    }

    @Override
    public List<GroupVO> findGroups() {
        UserSession session = SessionContext.getSession();
        // 查询当前用户的群id列表
        List<GroupMember> groupMembers = groupMemberService.findByUserId(session.getUserId());
        // 30天内退的群可能存在退群前的离线消息,一并返回作为前端缓存
        Date minQuitTime = DateUtils.addDays(new Date(), Math.toIntExact(-Constant.MAX_OFFLINE_MESSAGE_DAYS));
        groupMembers.addAll(groupMemberService.findQuitMembers(session.getUserId(),minQuitTime));
        if (groupMembers.isEmpty()) {
            return new LinkedList<>();
        }
        // 拉取群列表
        List<Long> ids = groupMembers.stream().map((GroupMember::getGroupId)).collect(Collectors.toList());
        LambdaQueryWrapper<Group> groupWrapper = Wrappers.lambdaQuery();
        groupWrapper.in(Group::getId, ids);
        List<Group> groups = this.list(groupWrapper);
        // 转vo
        return groups.stream().map(group -> {
            GroupMember member =
                groupMembers.stream().filter(m -> group.getId().equals(m.getGroupId())).findFirst().get();
            return convert(group, member);
        }).collect(Collectors.toList());
    }

    @RedisLock(prefixKey = RedisKey.IM_LOCK_GROUP_ENTER, key = "#dto.getGroupId()")
    @Override
    public void invite(GroupInviteDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = this.getAndCheckById(dto.getGroupId());
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getGroupId(), session.getUserId());
        if (Objects.isNull(group) || member.getQuit()) {
            throw new GlobalException("您不在群聊中,邀请失败");
        }
        boolean isOwner = session.getUserId().equals(group.getOwnerId());
        if (!group.getIsAllowInvite() && !isOwner && !member.getIsManager()) {
            throw new GlobalException("本群禁止普通成员邀请好友");
        }
        // 群聊人数校验
        List<GroupMember> members = groupMemberService.findByGroupId(dto.getGroupId(), 0L);
        long size = members.stream().filter(m -> !m.getQuit()).count();
        if (dto.getFriendIds().size() + size > Constant.MAX_LARGE_GROUP_MEMBER) {
            throw new GlobalException("群聊人数不能大于" + Constant.MAX_LARGE_GROUP_MEMBER + "人");
        }
        // 找出好友信息
        List<Friend> friends = friendsService.findByFriendIds(dto.getFriendIds());
        if (dto.getFriendIds().size() != friends.size()) {
            throw new GlobalException("部分用户不是您的好友，邀请失败");
        }
        // 批量保存成员数据
        List<GroupMember> groupMembers = friends.stream().map(f -> {
            Optional<GroupMember> optional =
                members.stream().filter(m -> m.getUserId().equals(f.getFriendId())).findFirst();
            GroupMember groupMember = optional.orElseGet(GroupMember::new);
            groupMember.setGroupId(dto.getGroupId());
            groupMember.setUserId(f.getFriendId());
            groupMember.setUserNickName(f.getFriendNickName());
            groupMember.setHeadImage(f.getFriendHeadImage());
            groupMember.setCompanyName(f.getFriendCompanyName());
            groupMember.setCreatedTime(new Date());
            groupMember.setIsManager(false);
            groupMember.setQuit(false);
            return groupMember;
        }).collect(Collectors.toList());
        if (!groupMembers.isEmpty()) {
            groupMemberService.saveOrUpdateBatch(group.getId(), groupMembers);
        }
        // 推送同步消息给被邀请人
        for (GroupMember groupMember : groupMembers) {
            GroupVO groupVo = convert(group, groupMember);
            sendAddGroupMessage(groupVo, List.of(groupMember.getUserId()));
        }
        // 推送进入群聊消息
        String content = String.format("%s邀请%s加入了群聊", formatUserMark(member), formatUserMarks(groupMembers));
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(dto.getGroupId());
        this.sendTipMessage(dto.getGroupId(), userIds, content, true);
        log.info("邀请进入群聊，群聊id:{},群聊名称:{},被邀请用户id:{}", group.getId(), group.getName(),
            dto.getFriendIds());
    }

    @Override
    public BizTokenVO generateShareCardToken(Long groupId) {
        UserSession session = SessionContext.getSession();
        Group group = this.getAndCheckById(groupId);
        // 检查用户是否在群聊中，只有群成员才能生成加入token
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        if (Objects.isNull(member) || member.getQuit()) {
            throw new GlobalException("您不在群聊中，无法生成名片");
        }
        // 检查群聊是否允许分享
        boolean isOwner = session.getUserId().equals(group.getOwnerId());
        if ((!isOwner && !member.getIsManager() && !group.getIsAllowShareCard())) {
            // 普通成员需要检查是否允许分享名片
            throw new GlobalException("本群禁止普通成员分享名片");
        }
        // 生成唯一token
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = StrUtil.join(":", RedisKey.IM_GROUP_CARD_TOKEN, token);
        GroupCardDTO card = new GroupCardDTO();
        card.setGroupId(groupId);
        card.setUserId(session.getUserId());
        redisTemplate.opsForValue().set(key, card, Constant.SHARE_CARD_EXPIRED_DAYS, TimeUnit.DAYS);
        // 返回token
        BizTokenVO vo = new BizTokenVO();
        vo.setToken(token);
        vo.setType(BizTokenType.GROUP_CARD.getCode());
        vo.setExpiredIn(DateUtils.addDays(new Date(), Math.toIntExact(Constant.SHARE_CARD_EXPIRED_DAYS)).getTime());
        log.info("生成群名片token，群聊id:{},用户id:{}", group.getId(), session.getUserId());
        return vo;
    }

    @Override
    public BizTokenVO generateQrcodeToken(Long groupId) {
        UserSession session = SessionContext.getSession();
        Group group = this.getAndCheckById(groupId);
        // 检查用户是否在群聊中，只有群成员才能生成加入token
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        if (Objects.isNull(member) || member.getQuit()) {
            throw new GlobalException("您不在群聊中，无法生成二维码");
        }
        // 检查群聊是否允许分享
        boolean isOwner = session.getUserId().equals(group.getOwnerId());
        if ((!isOwner && !member.getIsManager() && !group.getIsAllowInvite())) {
            // 普通成员需要检查是否允许分享名片
            throw new GlobalException("本群禁止普通成员生成二维码");
        }
        // 生成唯一token
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = StrUtil.join(":", RedisKey.IM_GROUP_QRCODE_TOKEN, token);
        GroupQrcodeDTO qrcode = new GroupQrcodeDTO();
        qrcode.setGroupId(groupId);
        qrcode.setUserId(session.getUserId());
        redisTemplate.opsForValue().set(key, qrcode, Constant.GROUP_QRCODE_EXPIRED_DAYS, TimeUnit.DAYS);
        // 返回vo
        BizTokenVO vo = new BizTokenVO();
        vo.setToken(token);
        vo.setType(BizTokenType.GROUP_QRCODE.getCode());
        vo.setExpiredIn(DateUtils.addDays(new Date(), Math.toIntExact(Constant.GROUP_QRCODE_EXPIRED_DAYS)).getTime());
        log.info("生成群聊二维码token，群聊id:{},用户id:{}", group.getId(), session.getUserId());
        return vo;
    }

    @RedisLock(prefixKey = RedisKey.IM_LOCK_GROUP_ENTER, key = "#dto.groupId")
    @Override
    public GroupVO join(GroupJoinDTO dto) {
        UserSession session = SessionContext.getSession();
        User user = userMapper.selectById(session.getUserId());
        Group group = this.getAndCheckById(dto.getGroupId());
        String content = "";
        BizTokenVO tokenVo = dto.getToken();
        // 名片分享校验
        if (BizTokenType.GROUP_CARD.getCode().equals(tokenVo.getType())) {
            String key = StrUtil.join(":", RedisKey.IM_GROUP_CARD_TOKEN, tokenVo.getToken());
            GroupCardDTO card = (GroupCardDTO)redisTemplate.opsForValue().get(key);
            if (Objects.isNull(card) || !dto.getGroupId().equals(card.getGroupId())) {
                throw new GlobalException("名片非法或已失效");
            }
            GroupMember member = groupMemberService.findByGroupAndUserId(card.getGroupId(), card.getUserId());
            content = String.format("%s通过%s分享的名片加入了群聊",
                formatUserMark(session.getUserId(), session.getNickName()), formatUserMark(member));
        }
        // 名片分享校验
        if (BizTokenType.GROUP_QRCODE.getCode().equals(tokenVo.getType())) {
            String key = StrUtil.join(":", RedisKey.IM_GROUP_QRCODE_TOKEN, tokenVo.getToken());
            GroupQrcodeDTO qrcode = (GroupQrcodeDTO)redisTemplate.opsForValue().get(key);
            if (Objects.isNull(qrcode) || !dto.getGroupId().equals(qrcode.getGroupId())) {
                throw new GlobalException("二维码非法或已失效");
            }
            GroupMember member = groupMemberService.findByGroupAndUserId(qrcode.getGroupId(), qrcode.getUserId());
            content = String.format("%s通过%s分享的二维码加入了群聊",
                formatUserMark(session.getUserId(), session.getNickName()), formatUserMark(member));
        }
        // 检查用户是否已在群聊中
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getGroupId(), session.getUserId());
        if (!Objects.isNull(member) && !member.getQuit()) {
            throw new GlobalException("您已在群聊中");
        }
        // 检查群聊人数限制
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(dto.getGroupId());
        if (userIds.size() >= Constant.MAX_LARGE_GROUP_MEMBER) {
            throw new GlobalException("群聊人数已达上限");
        }
        // 创建或更新群成员信息
        if (Objects.isNull(member)) {
            member = new GroupMember();
        }
        member.setGroupId(dto.getGroupId());
        member.setUserId(user.getId());
        member.setUserNickName(user.getNickName());
        member.setHeadImage(user.getHeadImageThumb());
        member.setCompanyName(user.getCompanyName());
        member.setCreatedTime(new Date());
        member.setIsManager(false);
        member.setQuit(false);
        groupMemberService.saveOrUpdate(member);
        GroupVO vo = convert(group, member);
        // 同步群聊列表
        sendAddGroupMessage(vo, List.of(session.getUserId()));
        // 推送提示语
        userIds.add(session.getUserId());
        this.sendTipMessage(dto.getGroupId(), userIds, content, true);
        log.info("加入群聊，群聊id:{},群聊名称:{},用户id:{}", group.getId(), group.getName(), session.getUserId());
        return vo;
    }

    @Override
    public List<GroupMemberVO> findGroupMembers(Long groupId, Long version) {
        Group group = getById(groupId);
        List<GroupMember> members = groupMemberService.findByGroupId(groupId, version);
        List<Long> userIds = members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
        Map<Long, String> friendRemarkMap = friendService.loadRemark(userIds);
        List<Long> onlineUserIds = imClient.getOnlineUser(userIds);
        return members.stream().map(m -> {
            GroupMemberVO vo = BeanUtils.copyProperties(m, GroupMemberVO.class);
            // 优先使用好友备注昵称
            vo.setShowNickName(
                friendRemarkMap.containsKey(m.getUserId()) ? friendRemarkMap.get(m.getUserId()) : m.getShowNickName());
            vo.setShowGroupName(StrUtil.blankToDefault(m.getRemarkGroupName(), group.getName()));
            vo.setOnline(onlineUserIds.contains(m.getUserId()));
            return vo;
        }).sorted((m1, m2) -> {
            // 在线的放前面
            if (!m1.getOnline().equals(m2.getOnline())) {
                return m2.getOnline().compareTo(m1.getOnline());
            }
            // 群主在前面
            if (m1.getUserId().equals(group.getOwnerId())) {
                return -1;
            }
            if (m2.getUserId().equals(group.getOwnerId())) {
                return 1;
            }
            // 管理员在前面
            return m2.getIsManager().compareTo(m1.getIsManager());
        }).collect(Collectors.toList());
    }

    @Override
    public List<Long> findOnlineMemberIds(Long groupId) {
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        return imClient.getOnlineUser(userIds);
    }

    @CacheEvict(key = "#dto.id")
    @Transactional
    @Override
    public void setGroupMuted(GroupMutedDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(dto.getId());
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getId(), session.getUserId());
        if (!session.getUserId().equals(group.getOwnerId()) && !member.getIsManager()) {
            throw new GlobalException("您没有操作权限");
        }
        if (!group.getIsAllMuted().equals(dto.getIsMuted())) {
            group.setIsAllMuted(dto.getIsMuted());
            this.updateById(group);
            // 推送提示语
            List<Long> userIds = groupMemberService.findUserIdsByGroupId(dto.getId());
            this.sendMutedTip(dto.getId(), member, userIds, dto.getIsMuted());
            // 推送同步消息
            sendSyncMessage(dto.getId(), userIds, MessageType.GROUP_ALL_MUTED, dto.getIsMuted());
        }
    }

    @Override
    public void setMemberMuted(GroupMemberMutedDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(dto.getGroupId());
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getGroupId(), session.getUserId());
        if (!session.getUserId().equals(group.getOwnerId()) && !member.getIsManager()) {
            throw new GlobalException("您没有操作权限");
        }
        if (dto.getUserIds().contains(group.getOwnerId())) {
            throw new GlobalException("不允许禁言群主");
        }
        // 过滤掉禁言状态无需修改的用户，避免出现重复推送提示消息
        List<Long> userIds = groupMemberService.findMutedUserIds(dto.getGroupId(), dto.getUserIds(), !dto.getIsMuted());
        if (!userIds.isEmpty()) {
            groupMemberService.setMuted(dto.getGroupId(), userIds, dto.getIsMuted());
            List<GroupMember> members = groupMemberService.findByGroupAndUserIds(dto.getGroupId(), userIds);
            String mutedTip = dto.getIsMuted() ? "开启" : "解除";
            String tip = String.format("%s%s了%s的禁言", formatUserMark(member), mutedTip, formatUserMarks(members));
            this.sendTipMessage(dto.getGroupId(), Collections.EMPTY_LIST, tip, true);
            // 推送同步消息
            sendSyncMessage(dto.getGroupId(), userIds, MessageType.GROUP_MEMBER_MUTED, dto.getIsMuted());
        }
    }

    @Transactional
    @Override
    public void setTopMessage(Long groupId, Long messageId) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(groupId);
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        if (!session.getUserId().equals(group.getOwnerId()) && !member.getIsManager()) {
            throw new GlobalException("您没有操作权限");
        }
        GroupMessage message = groupMessageMapper.selectById(messageId);
        if (Objects.isNull(message) || !message.getGroupId().equals(groupId)) {
            throw new GlobalException("消息不存在");
        }
        // 更新群置顶消息id
        group.setTopMessageId(messageId);
        this.updateById(group);
        // 更新用户置顶显示状态
        groupMemberService.updateTopMessage(groupId, true);
        // 推送提示语
        String content = String.format("%s置顶了一条消息", formatUserMark(member));
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        sendTipMessage(groupId, userIds, content, true);
        // 推送同步消息
        sendTopGroupMessage(groupId, userIds, message);
    }

    @Transactional
    @Override
    public void removeTopMessage(Long groupId) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(groupId);
        GroupMember member = groupMemberService.findByGroupAndUserId(groupId, session.getUserId());
        if (!session.getUserId().equals(group.getOwnerId()) && !member.getIsManager()) {
            throw new GlobalException("您没有操作权限");
        }
        // 清空置顶消息
        LambdaUpdateWrapper<Group> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Group::getId, groupId);
        wrapper.set(Group::getTopMessageId, null);
        this.update(wrapper);
        // 更新用户置顶显示状态
        groupMemberService.updateTopMessage(groupId, false);
        // 推送同步消息
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
        sendTopGroupMessage(groupId, userIds, null);
    }

    @Override
    public void hideTopMessage(Long groupId) {
        UserSession session = SessionContext.getSession();
        // 更新用户置顶显示状态
        groupMemberService.updateTopMessage(groupId, session.getUserId(), false);
        // 推送同步消息
        sendTopGroupMessage(groupId, List.of(session.getUserId()), null);
    }

    @Override
    public void addManager(GroupManagerDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(dto.getGroupId());
        if (!session.getUserId().equals(group.getOwnerId())) {
            throw new GlobalException("您没有操作权限");
        }
        groupMemberService.setManager(dto.getGroupId(), dto.getUserIds(), true);
        List<GroupMember> members = groupMemberService.findByGroupAndUserIds(dto.getGroupId(), dto.getUserIds());
        String tip = String.format("%s成为本群管理员", formatUserMarks(members));
        this.sendTipMessage(dto.getGroupId(), Collections.EMPTY_LIST, tip, true);
    }

    @Override
    public void removeManager(GroupManagerDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(dto.getGroupId());
        if (!session.getUserId().equals(group.getOwnerId())) {
            throw new GlobalException("您没有操作权限");
        }
        groupMemberService.setManager(dto.getGroupId(), dto.getUserIds(), false);
        List<GroupMember> members = groupMemberService.findByGroupAndUserIds(dto.getGroupId(), dto.getUserIds());
        String tip = String.format("%s被移除管理员身份", formatUserMarks(members));
        this.sendTipMessage(dto.getGroupId(), Collections.EMPTY_LIST, tip, true);
    }

    @Override
    public void setDnd(GroupDndDTO dto) {
        UserSession session = SessionContext.getSession();
        groupMemberService.setDnd(dto.getGroupId(), session.getUserId(), dto.getIsDnd());
        // 更新缓存
        String key = StrUtil.join(":", RedisKey.IM_CACHE_GROUP_DNS, session.getUserId());
        if (dto.getIsDnd()) {
            redisTemplate.opsForSet().add(key, dto.getGroupId());
        } else {
            redisTemplate.opsForSet().remove(key, dto.getGroupId());
        }
        // 推送同步消息
        sendSyncDndMessage(dto.getGroupId(), dto.getIsDnd());
    }

    @Override
    public void initDndCache(Long userId) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.eq(GroupMember::getIsDnd, true);
        wrapper.select(GroupMember::getGroupId);
        List<GroupMember> members = groupMemberService.list(wrapper);
        Object[] ids = members.stream().map(GroupMember::getGroupId).toArray();
        String key = StrUtil.join(":", RedisKey.IM_CACHE_GROUP_DNS, userId);
        redisTemplate.delete(key);
        if (ids.length > 0) {
            redisTemplate.opsForSet().add(key, ids);
        }
    }

    @Override
    public Boolean isDnd(Long userId, Long groupId) {
        String key = StrUtil.join(":", RedisKey.IM_CACHE_GROUP_DNS, userId);
        return redisTemplate.opsForSet().isMember(key, groupId);
    }

    @Override
    public void setTop(GroupTopDTO dto) {
        UserSession session = SessionContext.getSession();
        groupMemberService.setTop(dto.getGroupId(), session.getUserId(), dto.getIsTop());
        // 推送同步消息
        sendSyncTopMessage(dto.getGroupId(), dto.getIsTop());
    }

    @Override
    public void setAllowInvite(GroupAllowInviteDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(dto.getGroupId());
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getGroupId(), session.getUserId());
        if (!session.getUserId().equals(group.getOwnerId()) && !member.getIsManager()) {
            throw new GlobalException("您没有操作权限");
        }
        LambdaUpdateWrapper<Group> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Group::getId, dto.getGroupId());
        wrapper.set(Group::getIsAllowInvite, dto.getIsAllowInvite());
        this.update(wrapper);
    }

    @Override
    public void setAllowShareCard(GroupAllowShareCardDTO dto) {
        UserSession session = SessionContext.getSession();
        Group group = getAndCheckById(dto.getGroupId());
        GroupMember member = groupMemberService.findByGroupAndUserId(dto.getGroupId(), session.getUserId());
        if (!session.getUserId().equals(group.getOwnerId()) && !member.getIsManager()) {
            throw new GlobalException("您没有操作权限");
        }
        LambdaUpdateWrapper<Group> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(Group::getId, dto.getGroupId());
        wrapper.set(Group::getIsAllowShareCard, dto.getIsAllowShareCard());
        this.update(wrapper);
    }

    private void sendMutedTip(Long groupId, GroupMember member, List<Long> userIds, Boolean isMuted) {
        String name = formatUserMark(member);
        String tip = name + (isMuted ? "开启了全员禁言,只有群主和管理员可以发言" : "解除了全员禁言");
        this.sendTipMessage(groupId, userIds, tip, true);
    }

    private String formatUserMark(Long userId, String displayName) {
        // 格式：#{displayName:userId}，例如：#{张三:123}
        return String.format("#{%s:%d}", displayName, userId);
    }

    private String formatUserMark(GroupMember member) {
        // 格式：#{displayName:userId}，例如：#{张三:123}
        return String.format("#{%s:%d}", member.getShowNickName(), member.getUserId());
    }

    private String formatUserMarks(List<GroupMember> members) {
        // 格式：#{displayName1:userId1},#{displayName2:userId2}，例如：#{张三:123},#{李四:456}
        return members.stream().map(m -> formatUserMark(m)).collect(Collectors.joining(","));
    }

    private void sendTipMessage(Long groupId, List<Long> recvIds, String content, Boolean sendToAll) {
        UserSession session = SessionContext.getSession();
        // 消息入库
        GroupMessage message = new GroupMessage();
        message.setContent(content);
        message.setType(MessageType.TIP_TEXT.code());
        message.setStatus(MessageStatus.PENDING.code());
        message.setSendTime(new Date());
        message.setSendNickName(session.getNickName());
        message.setGroupId(groupId);
        message.setSendId(session.getUserId());
        message.setRecvIds(sendToAll ? "" : CommaTextUtils.asText(recvIds));
        groupMessageMapper.insert(message);
        // 推送
        GroupMessageVO msgInfo = BeanUtils.copyProperties(message, GroupMessageVO.class);
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        if (CollUtil.isEmpty(recvIds)) {
            // 为空表示向全体发送
            List<Long> userIds = groupMemberService.findUserIdsByGroupId(groupId);
            sendMessage.setRecvIds(userIds);
        } else {
            sendMessage.setRecvIds(recvIds);
        }
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        sendMessage.setSendToSelf(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private void sendAddGroupMessage(GroupVO group, List<Long> recvIds) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setContent(JSON.toJSONString(group));
        msgInfo.setType(MessageType.GROUP_NEW.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setGroupId(group.getId());
        msgInfo.setSendId(session.getUserId());
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvIds(recvIds);
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        sendMessage.setSendToSelf(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private void sendDelGroupMessage(Long groupId, List<Long> recvIds) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setType(MessageType.GROUP_DEL.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setGroupId(groupId);
        msgInfo.setSendId(session.getUserId());
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvIds(recvIds);
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        sendMessage.setSendToSelf(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private void sendSyncDndMessage(Long groupId, Boolean isDnd) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setType(MessageType.GROUP_DND.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setGroupId(groupId);
        msgInfo.setSendId(session.getUserId());
        msgInfo.setContent(isDnd.toString());
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private void sendSyncTopMessage(Long groupId, Boolean isTop) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setType(MessageType.GROUP_TOP.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setGroupId(groupId);
        msgInfo.setSendId(session.getUserId());
        msgInfo.setContent(isTop.toString());
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private void sendTopGroupMessage(Long groupId, List<Long> recvIds, GroupMessage message) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setType(MessageType.GROUP_TOP_MESSAGE.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setGroupId(groupId);
        msgInfo.setSendId(session.getUserId());
        if (!Objects.isNull(message)) {
            GroupMessageVO topMessage = BeanUtils.copyProperties(message, GroupMessageVO.class);
            msgInfo.setContent(JSON.toJSONString(topMessage));
        }
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvIds(recvIds);
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        sendMessage.setSendToSelf(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private void sendSyncMessage(Long groupId, List<Long> recvIds, MessageType type, Object content) {
        UserSession session = SessionContext.getSession();
        GroupMessageVO msgInfo = new GroupMessageVO();
        msgInfo.setType(type.code());
        msgInfo.setSendTime(new Date());
        msgInfo.setGroupId(groupId);
        msgInfo.setSendId(session.getUserId());
        msgInfo.setContent(JSON.toJSONString(content));
        IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
        sendMessage.setRecvIds(recvIds);
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        sendMessage.setSendToSelf(false);
        imClient.sendGroupMessage(sendMessage);
    }

    private GroupVO convert(Group group, GroupMember member) {
        GroupVO vo = BeanUtils.copyProperties(group, GroupVO.class);
        if (!Objects.isNull(member)) {
            vo.setRemarkGroupName(member.getRemarkGroupName());
            vo.setRemarkNickName(member.getRemarkNickName());
            vo.setShowNickName(member.getShowNickName());
            vo.setShowGroupName(StrUtil.blankToDefault(member.getRemarkGroupName(), group.getName()));
            vo.setQuit(member.getQuit());
            vo.setIsDnd(member.getIsDnd());
            vo.setIsTop(member.getIsTop());
            vo.setIsMuted(member.getIsMuted());
        } else {
            vo.setShowGroupName(group.getName());
            vo.setQuit(true);
            vo.setIsDnd(false);
            vo.setIsTop(false);
            vo.setIsMuted(false);
        }
        return vo;
    }
}
