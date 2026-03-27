package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.imclient.IMClient;
import com.bx.imcommon.model.IMPrivateMessage;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.dto.FriendRequestApplyDTO;
import com.bx.implatform.entity.Friend;
import com.bx.implatform.entity.FriendRequest;
import com.bx.implatform.entity.User;
import com.bx.implatform.enums.FriendRequestStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.FriendRequestMapper;
import com.bx.implatform.service.*;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.FriendRequestVO;
import com.bx.implatform.vo.PrivateMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 好友审核
 *
 * @author Blue
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest> implements FriendRequestService {

    private final UserService userService;
    private final FriendService friendService;
    private final GroupMemberService groupMemberService;
    private final UserBlacklistService userBlacklistService;
    private final IMClient imClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<FriendRequestVO> loadPendingList() {
        UserSession session = SessionContext.getSession();
        // 我收到的请求
        LambdaQueryWrapper<FriendRequest> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        wrapper.eq(FriendRequest::getRecvId, session.getUserId());
        wrapper.orderByDesc(FriendRequest::getId);
        List<FriendRequest> requests = this.list(wrapper);
        // 我发出的请求
        wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        wrapper.eq(FriendRequest::getSendId, session.getUserId());
        wrapper.orderByDesc(FriendRequest::getId);
        requests.addAll(this.list(wrapper));
        return requests.stream().map(o -> BeanUtils.copyProperties(o, FriendRequestVO.class)).collect(Collectors.toList());
    }

    @Override
    public FriendRequestVO apply(FriendRequestApplyDTO dto) {
        UserSession session = SessionContext.getSession();
        Long userId = session.getUserId();
        // 检查每日添加好友数量限制，防止脚本恶意添加
        //checkDailyApplyLimit(userId);
        Long friendId = dto.getFriendId();
        if (userId.equals(friendId)) {
            throw new GlobalException("不允许添加自己为好友");
        }
        //if (friendService.isFriend(userId, friendId)) {
        //    throw new GlobalException("对方已是您的好友");
        //}
        Friend friend = friendService.lambdaQuery()
                .select(Friend::getId, Friend::getDeleted)
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .last("limit 1")
                .one();
        if (friend != null && !friend.getDeleted()) {
            throw new GlobalException("对方已是您的好友");
        }
        if (userBlacklistService.isInBlacklist(friendId, userId)) {
            throw new GlobalException("对方已将您拉入黑名单");
        }
        if (friend == null && !groupMemberService.isAllowAdd(userId, friendId)) {
            throw new GlobalException("群内禁止添加好友");
        }
        // 先查询，防止多次重复申请
        boolean exists = this.lambdaQuery()
            .eq(FriendRequest::getSendId, userId)
            .eq(FriendRequest::getRecvId, friendId)
            .eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode())
            .exists();
        if (exists) {
            throw new GlobalException("您已在对方的好友申请列表中,无需重复申请");
        }
        // 对方申请列表数量上限校验
        //LambdaQueryWrapper<FriendRequest> wrapper = Wrappers.lambdaQuery();
        //wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        //wrapper.eq(FriendRequest::getRecvId, friendId);
        //if (this.count(wrapper) >= Constant.MAX_PRIEND_APPLY) {
        //    throw new GlobalException("对方的好友申请列表已满");
        //}
        // 自己的申请列表上限校验
        //wrapper = Wrappers.lambdaQuery();
        //wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        //wrapper.eq(FriendRequest::getSendId, userId);
        //if (this.count(wrapper) >= Constant.MAX_PRIEND_APPLY) {
        //    throw new GlobalException("您的好友申请列表已满");
        //}
        // 新请求
        List<User> userList = userService.lambdaQuery()
            .select(User::getId, User::getNickName, User::getHeadImageThumb, User::getIsManualApprove)
            .in(User::getId, userId, friendId)
            .eq(User::getCompanyId, session.getCompanyId())
            .list();
        User sender = null;
        User receiver = null;
        for (User user : userList) {
            if (Objects.equals(user.getId(), userId)) {
                sender = user;
            }
            if (Objects.equals(user.getId(), friendId)) {
                receiver = user;
            }
        }
        if (receiver == null) {
            throw new GlobalException("好友不存在");
        }
        FriendRequest request = new FriendRequest();
        request.setSendId(userId);
        request.setSendNickName(sender.getNickName());
        request.setSendHeadImage(sender.getHeadImageThumb());
        request.setRecvId(friendId);
        request.setRecvNickName(receiver.getNickName());
        request.setRecvHeadImage(receiver.getHeadImageThumb());
        request.setRemark(dto.getRemark());
        request.setApplyTime(new Date());
        request.setStatus(FriendRequestStatus.PENDING.getCode());
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        if (!receiver.getIsManualApprove()) {
            // 自动通过好友
            friendService.addFriend(session.getCompanyId(), friendId, userId, dto.getRemark());
            // 推送同意消息
            vo.setStatus(FriendRequestStatus.APPROVED.getCode());
        } else {
            // 入库
            this.saveOrUpdate(request);
            vo.setId(request.getId());
            // 推送添加请求
            sendRequestMessage(session.getUserId(), session.getTerminal(), friendId, MessageType.FRIEND_REQ_APPLY, vo, false, true);
        }
        // 增加每日申请好友计数
        //incrementDailyApplyCount(userId);
        return vo;
    }

    @Override
    public void approve(Long id) {
        UserSession session = SessionContext.getSession();
        FriendRequest request = this.getById(id);
        if (!session.getUserId().equals(request.getRecvId())) {
            throw new GlobalException("您无法处理该请求");
        }
        if (!FriendRequestStatus.PENDING.getCode().equals(request.getStatus())) {
            throw new GlobalException("该请求已处理");
        }
        // 添加好友
        friendService.addFriend(session.getCompanyId(), session.getUserId(), request.getSendId(), request.getRemark());
        // 更新状态
        this.lambdaUpdate()
            .eq(FriendRequest::getId, id)
            .set(FriendRequest::getStatus, FriendRequestStatus.APPROVED.getCode())
            .update();
        request.setStatus(FriendRequestStatus.APPROVED.getCode());
        // 推送同意消息
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        sendRequestMessage(session.getUserId(), session.getTerminal(), request.getSendId(), MessageType.FRIEND_REQ_APPROVE, vo, true, false);
    }

    @Override
    public void reject(Long id) {
        UserSession session = SessionContext.getSession();
        FriendRequest request = this.getById(id);
        if (!session.getUserId().equals(request.getRecvId())) {
            throw new GlobalException("您无法处理该请求");
        }
        if (!FriendRequestStatus.PENDING.getCode().equals(request.getStatus())) {
            throw new GlobalException("该请求已处理");
        }
        // 更新状态
        this.lambdaUpdate()
            .eq(FriendRequest::getId, id)
            .set(FriendRequest::getStatus, FriendRequestStatus.REJECTED.getCode())
            .update();
        request.setStatus(FriendRequestStatus.REJECTED.getCode());
        // 推送同意消息
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        sendRequestMessage(session.getUserId(), session.getTerminal(), request.getSendId(), MessageType.FRIEND_REQ_REJECT, vo, true, false);
    }

    @Override
    public void recall(Long id) {
        UserSession session = SessionContext.getSession();
        FriendRequest request = this.getById(id);
        if (!session.getUserId().equals(request.getSendId())) {
            throw new GlobalException("您无法处理该请求");
        }
        if (!FriendRequestStatus.PENDING.getCode().equals(request.getStatus())) {
            throw new GlobalException("该请求已处理，无法撤回");
        }
        // 更新状态
        this.lambdaUpdate()
                .eq(FriendRequest::getId, id)
                .set(FriendRequest::getStatus, FriendRequestStatus.RECALL.getCode())
                .update();
        request.setStatus(FriendRequestStatus.RECALL.getCode());
        // 推送同意消息
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        sendRequestMessage(session.getUserId(), session.getTerminal(), request.getRecvId(), MessageType.FRIEND_REQ_RECALL, vo, true, true);
    }

    void sendRequestMessage(Long userId, Integer terminal, Long fid, MessageType type, FriendRequestVO vo, Boolean sendToSelf, Boolean sendResult) {
        PrivateMessageVO msgInfo = new PrivateMessageVO();
        msgInfo.setSendId(userId);
        msgInfo.setRecvId(fid);
        msgInfo.setSendTime(vo.getApplyTime());
        msgInfo.setType(type.code());
        msgInfo.setContent(JSON.toJSONString(vo));
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(userId, terminal));
        sendMessage.setRecvId(fid);
        sendMessage.setData(msgInfo);
        sendMessage.setSendToSelf(sendToSelf);
        sendMessage.setSendResult(sendResult);
        imClient.sendPrivateMessage(sendMessage);
    }

    private void checkDailyApplyLimit(Long userId) {
        // 检查每日添加好友数量限制
        String key = StrUtil.join(":", RedisKey.IM_FRIEND_APPLY_COUNT, userId);
        Object countObj = redisTemplate.opsForValue().get(key);
        Long currentCount = Objects.isNull(countObj) ? 0 : Long.parseLong(countObj.toString());
        if (currentCount >= Constant.DAILY_FRIEND_APPLY_LIMIT) {
            throw new GlobalException(String.format("您今日添加好友数量已达上限", Constant.DAILY_FRIEND_APPLY_LIMIT));
        }
    }

    private void incrementDailyApplyCount(Long userId) {
        // 增加每日添加好友计数
        String key = StrUtil.join(":", RedisKey.IM_FRIEND_APPLY_COUNT, userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count.equals(1L)) {
            // 第一次设置时，设置过期时间为到第二天0点
            long secondsUntilMidnight = getSecondsUntilMidnight();
            redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
        }
    }

    private long getSecondsUntilMidnight() {
        // 计算到第二天0点的剩余秒数
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrowMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return ChronoUnit.SECONDS.between(now, tomorrowMidnight);
    }

}
