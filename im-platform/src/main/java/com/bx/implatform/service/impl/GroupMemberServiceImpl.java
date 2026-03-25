package com.bx.implatform.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.GroupMember;
import com.bx.implatform.mapper.GroupMemberMapper;
import com.bx.implatform.service.GroupMemberService;
import com.bx.implatform.vo.GroupInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = RedisKey.IM_CACHE_GROUP_MEMBER_ID)
@RequiredArgsConstructor
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements GroupMemberService {

    private final RedisTemplate<String, Object> redisTemplate;

    @CacheEvict(key = "#member.getGroupId()")
    @Override
    public boolean save(GroupMember member) {
        Long version = getNextVersion(member.getGroupId());
        member.setVersion(version);
        return super.save(member);
    }

    @CacheEvict(key = "#groupId")
    @Override
    public boolean saveOrUpdateBatch(Long groupId, List<GroupMember> members) {
        Long version = getNextVersion(groupId);
        members.forEach(m -> m.setVersion(version));
        return super.saveOrUpdateBatch(members);
    }

    @Override
    public GroupMember findByGroupAndUserId(Long groupId, Long userId, SFunction<GroupMember, ?>... columns) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ArrayUtil.isNotEmpty(columns), columns);
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        return this.getOne(wrapper);
    }

    @Override
    public List<GroupMember> findByGroupAndUserIds(Long groupId, List<Long> userIds) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.in(GroupMember::getUserId, userIds);
        return this.list(wrapper);
    }

    @Override
    public List<GroupMember> findByUserId(Long userId, SFunction<GroupMember, ?>... columns) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ArrayUtil.isNotEmpty(columns), columns);
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.eq(GroupMember::getQuit, false);
        return this.list(wrapper);
    }

    @Override
    public List<GroupMember> findQuitMembers(Long userId, Date minQuitTime, SFunction<GroupMember, ?>... columns) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.select(ArrayUtil.isNotEmpty(columns), columns);
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.eq(GroupMember::getQuit, true);
        wrapper.ge(GroupMember::getQuitTime, minQuitTime);
        return this.list(wrapper);
    }

    @Override
    public List<GroupMember> findByGroupId(Long groupId, Long version) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.gt(version > 0, GroupMember::getVersion, version);
        return this.list(wrapper);
    }

    @Cacheable(key = "#groupId")
    @Override
    public List<Long> findUserIdsByGroupId(Long groupId) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getQuit, false);
        wrapper.select(GroupMember::getUserId);
        List<GroupMember> members = this.list(wrapper);
        return members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
    }

    @CacheEvict(key = "#groupId")
    @Override
    public void removeByGroupId(Long groupId) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getQuit, false);
        wrapper.set(GroupMember::getQuit, true);
        wrapper.set(GroupMember::getQuitTime, new Date());
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @CacheEvict(key = "#groupId")
    @Override
    public void removeByGroupAndUserId(Long groupId, Long userId) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.eq(GroupMember::getQuit, false);
        wrapper.set(GroupMember::getQuit, true);
        wrapper.set(GroupMember::getQuitTime, new Date());
        wrapper.set(GroupMember::getIsManager, false);
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @CacheEvict(key = "#groupId")
    @Override
    public void removeByGroupAndUserIds(Long groupId, List<Long> userId) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getQuit, false);
        wrapper.in(GroupMember::getUserId, userId);
        wrapper.set(GroupMember::getQuit, true);
        wrapper.set(GroupMember::getQuitTime, new Date());
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @Override
    public Boolean isInGroup(Long groupId, List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return true;
        }
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getQuit, false);
        wrapper.in(GroupMember::getUserId, userIds);
        return userIds.size() == this.count(wrapper);
    }

    @Override
    public void setMuted(Long groupId, List<Long> userIds, Boolean isMuted) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.in(GroupMember::getUserId, userIds);
        wrapper.set(GroupMember::getIsMuted, isMuted);
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @Override
    public List<Long> findMutedUserIds(Long groupId, List<Long> userIds, Boolean isMuted) {
        LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.in(GroupMember::getUserId, userIds);
        wrapper.eq(GroupMember::getIsMuted, isMuted);
        wrapper.select(GroupMember::getUserId);
        List<GroupMember> members = this.list(wrapper);
        return members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
    }

    @Override
    public void updateTopMessage(Long groupId, Boolean isTopMessage) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.set(GroupMember::getIsTopMessage, isTopMessage);
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @Override
    public void updateTopMessage(Long groupId, Long userId, Boolean isTopMessage) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.set(GroupMember::getIsTopMessage, isTopMessage);
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @Override
    public void setManager(Long groupId, List<Long> userIds, Boolean isManager) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.in(GroupMember::getUserId, userIds);
        wrapper.set(GroupMember::getIsManager, isManager);
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @Override
    public void setDnd(Long groupId, Long userId, Boolean isDnd) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.set(GroupMember::getIsDnd, isDnd);
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @Override
    public void setTop(Long groupId, Long userId, Boolean isTop) {
        Long version = getNextVersion(groupId);
        LambdaUpdateWrapper<GroupMember> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(GroupMember::getGroupId, groupId);
        wrapper.eq(GroupMember::getUserId, userId);
        wrapper.set(GroupMember::getIsTop, isTop);
        wrapper.set(GroupMember::getVersion, version);
        this.update(wrapper);
    }

    @CacheEvict(key = "#member.getGroupId()")
    @Override
    public boolean saveOrUpdate(GroupMember member) {
        Long version = getNextVersion(member.getGroupId());
        member.setVersion(version);
        return super.saveOrUpdate(member);
    }

    @Override
    public boolean isAllowAdd(Long sendId, Long recvId) {
        Integer result = this.getBaseMapper().isNotAllowAdd(sendId, recvId);
        if (result == null) {
            return true;
        }
        return false;
    }

    @Override
    public List<GroupInfoVO> findSameGroups(Long userId, Long friendId) {
        return this.getBaseMapper().findSameGroups(userId, friendId);
    }

    /**
     * 获取下一个成员版本号(每次递增)
     *
     * @param groupId 群id
     * @return
     */
    @Override
    public Long getNextVersion(Long groupId) {
        String key = StrUtil.join(":", RedisKey.IM_GROUP_MEMBER_MAX_VERSION, groupId);
        if (redisTemplate.hasKey(key)) {
            return redisTemplate.opsForValue().increment(key);
        } else {
            LambdaQueryWrapper<GroupMember> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(GroupMember::getGroupId, groupId);
            wrapper.orderByDesc(GroupMember::getVersion);
            wrapper.last("limit 1");
            GroupMember member = this.getOne(wrapper);
            Long version = Objects.isNull(member) ? 1 : member.getVersion() + 1;
            redisTemplate.opsForValue().set(key, version);
            return version;
        }
    }

}
