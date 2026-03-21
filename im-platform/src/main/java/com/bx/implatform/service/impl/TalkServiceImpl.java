package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.dto.TalkAddDTO;
import com.bx.implatform.dto.TalkCommentDTO;
import com.bx.implatform.dto.TalkQuery;
import com.bx.implatform.dto.TalkUpdateDTO;
import com.bx.implatform.entity.Talk;
import com.bx.implatform.entity.TalkComment;
import com.bx.implatform.entity.TalkLike;
import com.bx.implatform.entity.User;
import com.bx.implatform.mapper.TalkCommentMapper;
import com.bx.implatform.mapper.TalkLikeMapper;
import com.bx.implatform.mapper.TalkMapper;
import com.bx.implatform.mapper.UserMapper;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.service.FriendService;
import com.bx.implatform.service.TalkService;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.vo.TalkCommentVO;
import com.bx.implatform.vo.TalkDetailListVO;
import com.bx.implatform.vo.TalkRemindVO;
import com.bx.implatform.vo.TalkStarVO;
import com.bx.implatform.vo.TalkVO;
import com.bx.implatform.vo.TalkDetailVO;
import com.bx.implatform.vo.UserShowVO;
import com.bx.implatform.util.SensitiveFilterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * 朋友圈动态服务实现
 *
 * @author im-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TalkServiceImpl extends ServiceImpl<TalkMapper, Talk> implements TalkService {

    private final SensitiveFilterUtil sensitiveFilterUtil;
    private final FriendService friendService;
    private final UserMapper userMapper;
    private final TalkLikeMapper talkLikeMapper;
    private final TalkCommentMapper talkCommentMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(TalkAddDTO dto) {
        Long userId = SessionContext.getSession().getUserId();
        Talk talk = new Talk();
        talk.setUserId(userId);
        talk.setVisibleScope(dto.getVisibleScope());

        // 敏感词过滤
        if (StrUtil.isNotBlank(dto.getContent())) {
            talk.setContent(sensitiveFilterUtil.filter(dto.getContent()));
        }

        if (StrUtil.isNotBlank(dto.getAddress())) {
            talk.setAddress(dto.getAddress());
        }

        if (dto.getLatitude() != null) {
            talk.setLatitude(BigDecimal.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null) {
            talk.setLongitude(BigDecimal.valueOf(dto.getLongitude()));
        }

        // 附件列表序列化为JSON存储
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            talk.setFiles(JSON.toJSONString(dto.getFiles()));
        }

        this.save(talk);
        log.info("用户{}发布动态成功, talkId={}", userId, talk.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTalk(TalkUpdateDTO dto) {
        Long userId = SessionContext.getSession().getUserId();
        Talk talk = this.getById(dto.getId());
        if (talk == null) {
            throw new GlobalException("动态不存在");
        }
        if (!userId.equals(talk.getUserId())) {
            throw new GlobalException("无权限修改该动态");
        }
        talk.setVisibleScope(dto.getVisibleScope());
        boolean ok = this.updateById(talk);
        if (ok) {
            log.info("用户{}更新动态{}可见范围为{}", userId, dto.getId(), dto.getVisibleScope());
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTalk(Long talkId) {
        Long userId = SessionContext.getSession().getUserId();
        Talk talk = this.getById(talkId);
        if (talk == null) {
            throw new GlobalException("动态不存在");
        }
        if (!userId.equals(talk.getUserId())) {
            throw new GlobalException("无权限删除该动态");
        }
        this.removeById(talkId);
        log.info("用户{}删除动态{}", userId, talkId);
    }

    @Override
    public TalkDetailListVO query(TalkQuery query) {
        Long currentUserId = SessionContext.getSession().getUserId();
        List<Long> friendIds = friendService.findAllFriends().stream()
            .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
            .map(f -> f.getFriendId())
            .collect(Collectors.toList());

        LambdaQueryWrapper<Talk> wrapper = Wrappers.lambdaQuery();
        wrapper.and(w -> {
            w.eq(Talk::getUserId, currentUserId)
                .or()
                .eq(Talk::getVisibleScope, 9);
            if (!friendIds.isEmpty()) {
                w.or(w2 -> w2.eq(Talk::getVisibleScope, 2).in(Talk::getUserId, friendIds));
            }
        });
        if (query.getLastMinId() != null) {
            wrapper.lt(Talk::getId, query.getLastMinId());
        }
        wrapper.orderByDesc(Talk::getId);
        wrapper.last("LIMIT " + query.getPrefetchSize());

        List<Talk> talks = this.list(wrapper);
        if (talks.isEmpty()) {
            TalkDetailListVO vo = new TalkDetailListVO();
            vo.setDetaiList(Collections.emptyList());
            vo.setUserShowMap(Collections.emptyMap());
            return vo;
        }

        Set<Long> userIds = talks.stream().map(Talk::getUserId).collect(Collectors.toSet());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, UserShowVO> userShowMap = new HashMap<>();
        for (User u : users) {
            UserShowVO uv = new UserShowVO();
            uv.setId(u.getId());
            uv.setNickName(u.getNickName());
            uv.setHeadImageThumb(u.getHeadImageThumb());
            userShowMap.put(u.getId(), uv);
        }

        List<TalkDetailVO> detailList = new ArrayList<>();
        for (Talk t : talks) {
            TalkDetailVO d = new TalkDetailVO();
            d.setId(t.getId());
            d.setUserId(t.getUserId());
            d.setContent(t.getContent());
            d.setFiles(t.getFiles());
            d.setVisibleScope(t.getVisibleScope());
            d.setAddress(t.getAddress());
            d.setLatitude(t.getLatitude());
            d.setLongitude(t.getLongitude());
            d.setIsOwner(currentUserId.equals(t.getUserId()));
            d.setCreateTime(t.getCreateTime());
            d.setTalkStarVOS(Collections.emptyList());
            d.setTalkCommentVOS(Collections.emptyList());
            UserShowVO authorShow = userShowMap.get(t.getUserId());
            d.setUserShowMap(authorShow != null
                ? Collections.singletonMap(t.getUserId(), authorShow)
                : Collections.emptyMap());
            detailList.add(d);
        }

        TalkDetailListVO vo = new TalkDetailListVO();
        vo.setDetaiList(detailList);
        vo.setUserShowMap(userShowMap);
        return vo;
    }

    @Override
    public List<TalkVO> queryFriend(TalkQuery query) {
        List<Long> friendIds = friendService.findAllFriends().stream()
            .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
            .map(f -> f.getFriendId())
            .collect(Collectors.toList());
        if (friendIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Talk> wrapper = Wrappers.lambdaQuery();
        wrapper.in(Talk::getUserId, friendIds)
            .and(w -> w.eq(Talk::getVisibleScope, 2).or().eq(Talk::getVisibleScope, 9));
        if (query.getLastMinId() != null) {
            wrapper.lt(Talk::getId, query.getLastMinId());
        }
        wrapper.orderByDesc(Talk::getId);
        wrapper.last("LIMIT " + query.getPrefetchSize());

        List<Talk> talks = this.list(wrapper);
        return talks.stream().map(this::toTalkVO).collect(Collectors.toList());
    }

    @Override
    public List<TalkVO> querySelf(TalkQuery query) {
        Long userId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<Talk> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Talk::getUserId, userId);
        if (query.getLastMinId() != null) {
            wrapper.lt(Talk::getId, query.getLastMinId());
        }
        wrapper.orderByDesc(Talk::getId);
        wrapper.last("LIMIT " + query.getPrefetchSize());

        List<Talk> talks = this.list(wrapper);
        return talks.stream().map(this::toTalkVO).collect(Collectors.toList());
    }

    @Override
    public TalkDetailVO getDetail(Long talkId) {
        Long currentUserId = SessionContext.getSession().getUserId();
        Talk talk = this.getById(talkId);
        if (talk == null) {
            throw new GlobalException("动态不存在");
        }
        if (!canViewTalk(talk, currentUserId)) {
            throw new GlobalException("无权限查看该动态");
        }

        TalkDetailVO vo = new TalkDetailVO();
        vo.setId(talk.getId());
        vo.setUserId(talk.getUserId());
        vo.setContent(talk.getContent());
        vo.setFiles(talk.getFiles());
        vo.setVisibleScope(talk.getVisibleScope());
        vo.setAddress(talk.getAddress());
        vo.setLatitude(talk.getLatitude());
        vo.setLongitude(talk.getLongitude());
        vo.setIsOwner(currentUserId.equals(talk.getUserId()));
        vo.setCreateTime(talk.getCreateTime());

        LambdaQueryWrapper<TalkLike> likeW = Wrappers.lambdaQuery();
        likeW.eq(TalkLike::getTalkId, talkId).orderByAsc(TalkLike::getCreatedTime);
        List<TalkLike> likes = talkLikeMapper.selectList(likeW);
        vo.setTalkStarVOS(likes.stream().map(l -> toTalkStarVO(l, currentUserId)).collect(Collectors.toList()));

        LambdaQueryWrapper<TalkComment> commentW = Wrappers.lambdaQuery();
        commentW.eq(TalkComment::getTalkId, talkId).orderByAsc(TalkComment::getCreatedTime);
        List<TalkComment> comments = talkCommentMapper.selectList(commentW);
        vo.setTalkCommentVOS(comments.stream().map(c -> toTalkCommentVO(c, currentUserId)).collect(Collectors.toList()));

        Set<Long> userIds = new HashSet<>();
        userIds.add(talk.getUserId());
        likes.forEach(l -> userIds.add(l.getUserId()));
        comments.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getReplyUserId() != null) {
                userIds.add(c.getReplyUserId());
            }
        });
        Map<Long, UserShowVO> userShowMap = new HashMap<>();
        for (User u : userMapper.selectBatchIds(userIds)) {
            UserShowVO uv = new UserShowVO();
            uv.setId(u.getId());
            uv.setNickName(u.getNickName());
            uv.setHeadImageThumb(u.getHeadImageThumb());
            userShowMap.put(u.getId(), uv);
        }
        vo.setUserShowMap(userShowMap);
        return vo;
    }

    private TalkStarVO toTalkStarVO(TalkLike l, Long currentUserId) {
        TalkStarVO vo = new TalkStarVO();
        vo.setId(l.getId());
        vo.setTalkId(l.getTalkId());
        vo.setUserId(l.getUserId());
        vo.setCreateTime(l.getCreatedTime());
        vo.setIsOwner(currentUserId.equals(l.getUserId()));
        return vo;
    }

    private TalkCommentVO toTalkCommentVO(TalkComment c, Long currentUserId) {
        TalkCommentVO vo = new TalkCommentVO();
        vo.setId(c.getId());
        vo.setTalkId(c.getTalkId());
        vo.setUserId(c.getUserId());
        vo.setContent(c.getContent());
        vo.setReplyCommentId(c.getReplyCommentId());
        vo.setReplyUserId(c.getReplyUserId());
        vo.setType(c.getType());
        vo.setCreateTime(c.getCreatedTime());
        vo.setIsOwner(currentUserId.equals(c.getUserId()));
        return vo;
    }

    private TalkVO toTalkVO(Talk t) {
        TalkVO vo = new TalkVO();
        vo.setId(t.getId());
        vo.setContent(t.getContent());
        vo.setFiles(t.getFiles());
        vo.setAddress(t.getAddress());
        vo.setLatitude(t.getLatitude());
        vo.setLongitude(t.getLongitude());
        vo.setCreateTime(t.getCreateTime());
        return vo;
    }

    @Override
    public void markReadNotify() {
        Long userId = SessionContext.getSession().getUserId();
        String key = StrUtil.join(":", RedisKey.IM_TALK_READED_TIME, userId);
        redisTemplate.opsForValue().set(key, System.currentTimeMillis());
        log.debug("用户{}已读动态提醒", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long talkId) {
        Long userId = SessionContext.getSession().getUserId();
        Talk talk = this.getById(talkId);
        if (talk == null) {
            throw new GlobalException("动态不存在");
        }
        // 仅可对可见的动态点赞
        if (!canViewTalk(talk, userId)) {
            throw new GlobalException("无权限操作该动态");
        }
        LambdaQueryWrapper<TalkLike> exist = Wrappers.lambdaQuery();
        exist.eq(TalkLike::getTalkId, talkId).eq(TalkLike::getUserId, userId);
        if (talkLikeMapper.selectCount(exist) > 0) {
            return; // 已点赞，幂等返回成功
        }
        TalkLike like = new TalkLike();
        like.setTalkId(talkId);
        like.setUserId(userId);
        talkLikeMapper.insert(like);
        log.info("用户{}点赞动态{}", userId, talkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(TalkCommentDTO dto) {
        Long userId = SessionContext.getSession().getUserId();
        Talk talk = this.getById(dto.getTalkId());
        if (talk == null) {
            throw new GlobalException("动态不存在");
        }
        if (!canViewTalk(talk, userId)) {
            throw new GlobalException("无权限操作该动态");
        }
        Long replyUserId = null;
        if (dto.getReplyCommentId() != null) {
            TalkComment reply = talkCommentMapper.selectById(dto.getReplyCommentId());
            if (reply == null || !reply.getTalkId().equals(dto.getTalkId())) {
                throw new GlobalException("回复的评论不存在");
            }
            replyUserId = reply.getUserId();
        }
        String content = sensitiveFilterUtil.filter(dto.getContent());
        if (StrUtil.isBlank(content)) {
            throw new GlobalException("评论内容不可为空");
        }
        TalkComment comment = new TalkComment();
        comment.setTalkId(dto.getTalkId());
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setReplyCommentId(dto.getReplyCommentId());
        comment.setReplyUserId(replyUserId);
        comment.setType(dto.getType());
        talkCommentMapper.insert(comment);
        log.info("用户{}评论动态{}, commentId={}", userId, dto.getTalkId(), comment.getId());
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long userId = SessionContext.getSession().getUserId();
        TalkComment comment = talkCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new GlobalException("评论不存在");
        }
        if (!userId.equals(comment.getUserId())) {
            throw new GlobalException("无权限删除该评论");
        }
        talkCommentMapper.deleteById(commentId);
        log.info("用户{}删除评论{}", userId, commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long talkId) {
        Long userId = SessionContext.getSession().getUserId();
        LambdaQueryWrapper<TalkLike> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TalkLike::getTalkId, talkId).eq(TalkLike::getUserId, userId);
        talkLikeMapper.delete(wrapper);
        log.info("用户{}取消点赞动态{}", userId, talkId);
    }

    @Override
    public TalkRemindVO loadOfflineTalkRemind() {
        Long userId = SessionContext.getSession().getUserId();
        TalkRemindVO vo = new TalkRemindVO();
        vo.setNotifyCount(0L);
        vo.setTalkId(0L);
        vo.setAvatar("");

        List<Long> myTalkIds = getMyTalkIds(userId);
        if (myTalkIds.isEmpty()) {
            return vo;
        }

        long lastReadTime = 0L;
        String key = StrUtil.join(":", RedisKey.IM_TALK_READED_TIME, userId);
        Object val = redisTemplate.opsForValue().get(key);
        if (val != null) {
            lastReadTime = ((Number) val).longValue();
        }
        Date since = new Date(lastReadTime);

        LambdaQueryWrapper<TalkLike> likeW = Wrappers.lambdaQuery();
        likeW.in(TalkLike::getTalkId, myTalkIds)
            .ne(TalkLike::getUserId, userId)
            .gt(TalkLike::getCreatedTime, since);
        List<TalkLike> unreadLikes = talkLikeMapper.selectList(likeW);

        LambdaQueryWrapper<TalkComment> commentW = Wrappers.lambdaQuery();
        commentW.in(TalkComment::getTalkId, myTalkIds)
            .ne(TalkComment::getUserId, userId)
            .gt(TalkComment::getCreatedTime, since);
        List<TalkComment> unreadComments = talkCommentMapper.selectList(commentW);

        long count = unreadLikes.size() + unreadComments.size();
        vo.setNotifyCount(count);

        if (count == 0) {
            return vo;
        }

        TalkLike latestLike = unreadLikes.stream()
            .max((a, b) -> a.getCreatedTime().compareTo(b.getCreatedTime()))
            .orElse(null);
        TalkComment latestComment = unreadComments.stream()
            .max((a, b) -> a.getCreatedTime().compareTo(b.getCreatedTime()))
            .orElse(null);

        Long latestTalkId;
        Long actorUserId;
        if (latestLike != null && latestComment != null) {
            if (latestLike.getCreatedTime().after(latestComment.getCreatedTime())) {
                latestTalkId = latestLike.getTalkId();
                actorUserId = latestLike.getUserId();
            } else {
                latestTalkId = latestComment.getTalkId();
                actorUserId = latestComment.getUserId();
            }
        } else if (latestLike != null) {
            latestTalkId = latestLike.getTalkId();
            actorUserId = latestLike.getUserId();
        } else if (latestComment != null) {
            latestTalkId = latestComment.getTalkId();
            actorUserId = latestComment.getUserId();
        } else {
            return vo;
        }

        vo.setTalkId(latestTalkId);
        User actor = userMapper.selectById(actorUserId);
        if (actor != null && StrUtil.isNotBlank(actor.getHeadImageThumb())) {
            vo.setAvatar(actor.getHeadImageThumb());
        } else if (actor != null && StrUtil.isNotBlank(actor.getHeadImage())) {
            vo.setAvatar(actor.getHeadImage());
        }
        return vo;
    }

    private List<Long> getMyTalkIds(Long userId) {
        LambdaQueryWrapper<Talk> w = Wrappers.lambdaQuery();
        w.eq(Talk::getUserId, userId).select(Talk::getId);
        return this.list(w).stream().map(Talk::getId).collect(Collectors.toList());
    }

    /** 判断当前用户是否可见该动态 */
    private boolean canViewTalk(Talk talk, Long currentUserId) {
        if (currentUserId.equals(talk.getUserId())) {
            return true;
        }
        if (talk.getVisibleScope() == 9) {
            return true;
        }
        if (talk.getVisibleScope() == 2) {
            return friendService.isFriend(talk.getUserId(), currentUserId);
        }
        return false;
    }
}
