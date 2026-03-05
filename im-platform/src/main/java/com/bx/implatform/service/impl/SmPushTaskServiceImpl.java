package com.bx.implatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.SmPushTask;
import com.bx.implatform.enums.SmPushStatus;
import com.bx.implatform.mapper.SmPushTaskMapper;
import com.bx.implatform.service.SmPushTaskService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 系统消息推送任务
 *
 * @author: Blue
 * @date: 2024-09-07
 * @version: 1.0
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SmPushTaskServiceImpl extends ServiceImpl<SmPushTaskMapper, SmPushTask> implements SmPushTaskService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public SmPushTask findOneReadyTask() {
        LambdaQueryWrapper<SmPushTask> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SmPushTask::getStatus, SmPushStatus.WAIT_SEND.getValue());
        wrapper.le(SmPushTask::getSendTime, new Date());
        wrapper.orderByAsc(SmPushTask::getSendTime);
        wrapper.last("limit 1");
        return this.getOne(wrapper);
    }

    @Override
    public List<SmPushTask> findSendedTaskInDays(Long minSeqNo, int days) {
        UserSession session = SessionContext.getSession();
        Date minDate = DateUtils.addDays(new Date(), -days);
        LambdaQueryWrapper<SmPushTask> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SmPushTask::getStatus, SmPushStatus.SENDED.getValue());
        wrapper.gt(SmPushTask::getSeqNo, minSeqNo);
        wrapper.gt(SmPushTask::getSendTime, minDate);
        // 过滤不需要接收的消息
        String findInSet = String.format("FIND_IN_SET('%d',recv_ids)", session.getUserId());
        wrapper.and(wrap -> wrap.eq(SmPushTask::getSendToAll, true).or(w -> w.apply(findInSet)));
        wrapper.orderByAsc(SmPushTask::getSeqNo);
        return this.list(wrapper);
    }

    @Override
    public Long nextSeqNo() {
        if (redisTemplate.hasKey(RedisKey.IM_SM_TASK_SEQ)) {
            return redisTemplate.opsForValue().increment(RedisKey.IM_SM_TASK_SEQ);
        }
        // 如果redis被清理了，则去查询数据库中的最大seqno
        LambdaQueryWrapper<SmPushTask> wrapper = Wrappers.lambdaQuery();
        wrapper.isNotNull(SmPushTask::getSeqNo);
        wrapper.orderByDesc(SmPushTask::getSeqNo);
        wrapper.last("limit 1");
        SmPushTask task = this.getOne(wrapper);
        Long seqNo = Objects.isNull(task) ? 1L : task.getSeqNo() + 1;
        redisTemplate.opsForValue().set(RedisKey.IM_SM_TASK_SEQ, seqNo);
        return seqNo;
    }
}