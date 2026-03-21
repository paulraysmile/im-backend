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
import com.bx.implatform.entity.FriendRequest;
import com.bx.implatform.entity.User;
import com.bx.implatform.enums.FriendRequestStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.FriendRequestMapper;
import com.bx.implatform.service.FriendRequestService;
import com.bx.implatform.service.FriendService;
import com.bx.implatform.service.GroupService;
import com.bx.implatform.service.UserBlacklistService;
import com.bx.implatform.service.UserService;
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
public class FriendRequestServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest>
    implements FriendRequestService {

    private final UserService userService;
    private final FriendService friendService;
    private final GroupService groupService;
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
        return requests.stream().map(o -> BeanUtils.copyProperties(o, FriendRequestVO.class))
            .collect(Collectors.toList());
    }

    @Override
    public FriendRequestVO apply(FriendRequestApplyDTO dto) {
        UserSession session = SessionContext.getSession();
        // 检查每日添加好友数量限制，防止脚本恶意添加
        checkDailyApplyLimit(session.getUserId());
        if (session.getUserId().equals(dto.getFriendId())) {
            throw new GlobalException("不允许添加自己为好友");
        }
        if (friendService.isFriend(session.getUserId(), dto.getFriendId())) {
            throw new GlobalException("对方已是您的好友");
        }
        if (userBlacklistService.isInBlacklist(dto.getFriendId(), session.getUserId())) {
            throw new GlobalException("对方已将您拉入黑名单");
        }
        groupService.checkAllowAddFriendFromGroup(session.getUserId(), dto.getFriendId());
        // 先查询，防止多次重复申请
        LambdaQueryWrapper<FriendRequest> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FriendRequest::getSendId, session.getUserId());
        wrapper.eq(FriendRequest::getRecvId, dto.getFriendId());
        wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        FriendRequest request = this.getOne(wrapper);
        if (!Objects.isNull(request)) {
            throw new GlobalException("您已在对方的好友申请列表中,无需重复申请");
        }
        // 对方申请列表数量上限校验
        wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        wrapper.eq(FriendRequest::getRecvId, dto.getFriendId());
        if (this.count(wrapper) >= Constant.MAX_PRIEND_APPLY) {
            throw new GlobalException("对方的好友申请列表已满");
        }
        // 自己的申请列表上限校验
        wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        wrapper.eq(FriendRequest::getSendId, session.getUserId());
        if (this.count(wrapper) >= Constant.MAX_PRIEND_APPLY) {
            throw new GlobalException("您的好友申请列表已满");
        }
        // 新请求
        User sender = userService.getById(session.getUserId());
        User receiver = userService.getById(dto.getFriendId());
        request = new FriendRequest();
        request.setSendId(session.getUserId());
        request.setSendNickName(sender.getNickName());
        request.setSendHeadImage(sender.getHeadImageThumb());
        request.setRecvId(dto.getFriendId());
        request.setRecvNickName(receiver.getNickName());
        request.setRecvHeadImage(receiver.getHeadImageThumb());
        request.setRemark(dto.getRemark());
        request.setApplyTime(new Date());
        request.setStatus(FriendRequestStatus.PENDING.getCode());
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        if (!receiver.getIsManualApprove()) {
            // 自动通过好友
            friendService.addFriend(dto.getFriendId(), session.getUserId(), dto.getRemark());
            // 推送同意消息
            vo.setStatus(FriendRequestStatus.APPROVED.getCode());
        } else {
            // 入库
            this.saveOrUpdate(request);
            vo.setId(request.getId());
            // 推送添加请求
            sendRequestMessage(dto.getFriendId(), MessageType.FRIEND_REQ_APPLY, vo, false, true);
        }
        // 增加每日申请好友计数
        incrementDailyApplyCount(session.getUserId());
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
        friendService.addFriend(session.getUserId(), request.getSendId(), request.getRemark());
        // 更新状态
        request.setStatus(FriendRequestStatus.APPROVED.getCode());
        this.updateById(request);
        // 推送同意消息
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        sendRequestMessage(request.getSendId(), MessageType.FRIEND_REQ_APPROVE, vo, true, false);
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
        request.setStatus(FriendRequestStatus.REJECTED.getCode());
        this.updateById(request);
        // 推送同意消息
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        sendRequestMessage(request.getSendId(), MessageType.FRIEND_REQ_REJECT, vo, true, false);
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
        request.setStatus(FriendRequestStatus.RECALL.getCode());
        this.updateById(request);
        // 推送同意消息
        FriendRequestVO vo = BeanUtils.copyProperties(request, FriendRequestVO.class);
        sendRequestMessage(request.getRecvId(), MessageType.FRIEND_REQ_RECALL, vo, true, true);
    }

    void sendRequestMessage(Long fid, MessageType type, FriendRequestVO vo, Boolean sendToSelf, Boolean sendResult) {
        UserSession session = SessionContext.getSession();
        PrivateMessageVO msgInfo = new PrivateMessageVO();
        msgInfo.setSendId(session.getUserId());
        msgInfo.setRecvId(fid);
        msgInfo.setSendTime(vo.getApplyTime());
        msgInfo.setType(type.code());
        msgInfo.setContent(JSON.toJSONString(vo));
        IMPrivateMessage<PrivateMessageVO> sendMessage = new IMPrivateMessage<>();
        sendMessage.setSender(new IMUserInfo(session.getUserId(), session.getTerminal()));
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
