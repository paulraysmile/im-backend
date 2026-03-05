package com.bx.implatform.task.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bx.imclient.IMClient;
import com.bx.imcommon.model.IMSystemMessage;
import com.bx.imcommon.util.CommaTextUtils;
import com.bx.implatform.annotation.RedisLock;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.SmPushTask;
import com.bx.implatform.entity.SystemMessage;
import com.bx.implatform.entity.User;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.enums.SmPushStatus;
import com.bx.implatform.service.SmPushTaskService;
import com.bx.implatform.service.SystemMessageService;
import com.bx.implatform.service.UserService;
import com.bx.implatform.vo.SystemMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统消息推送任务
 *
 * @author: Blue
 * @date: 2024-09-07
 * @version: 1.0
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMessagePushTask {

    private final SmPushTaskService smPushTaskService;
    private final SystemMessageService systemMessageService;
    private final UserService userService;
    private final IMClient imClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private final int batchSize = 1000;

    @RedisLock(prefixKey = RedisKey.IM_LOCK_SM_TASK)
    @Scheduled(fixedRate = 30,timeUnit = TimeUnit.SECONDS)
    public void run() {
        log.info("【定时任务】系统消息推送任务...");
        SmPushTask task = smPushTaskService.findOneReadyTask();
        if (Objects.isNull(task)) {
            return;
        }
        SystemMessage msg = systemMessageService.getById(task.getMessageId());
        if (Objects.isNull(msg)) {
            return;
        }
        // 分配推送序列号,修改状态
        Long seqNo = smPushTaskService.nextSeqNo();
        task.setSeqNo(seqNo);
        task.setStatus(SmPushStatus.SENDING.getValue());
        smPushTaskService.updateById(task);
        if(task.getSendToAll()){
            // 发送给全体用户,对用户进行扫表
            Long minId = 0L;
            while (true) {
                List<Long> userIds = loadUserBatch(minId);
                // 推送
                sendMessage(userIds, task, msg);
                if (userIds.size() < batchSize) {
                    break;
                }
                // 加载下一批用户进行推送
                minId = userIds.get(batchSize - 1);
            }
        }else {
            // 指定用户
            List<String> strUserIds = CommaTextUtils.asList(task.getRecvIds());
            List<Long> userIds = strUserIds.stream().map(Long::parseLong).collect(Collectors.toList());
            // 推送
            sendMessage(userIds, task, msg);
        }
        // 推送完成
        task.setStatus(SmPushStatus.SENDED.getValue());
        smPushTaskService.updateById(task);
    }

    private List<Long> loadUserBatch(Long minId) {
        // 只推给一个月内活跃的用户
        Date minDate = DateUtils.addMonths(new Date(), -1);
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.gt(User::getLastLoginTime, minDate);
        wrapper.gt(User::getId, minId);
        wrapper.orderByAsc(User::getId);
        wrapper.select(User::getId);
        wrapper.last("limit " + batchSize);
        List<User> users = userService.list(wrapper);
        return users.stream().map(User::getId).collect(Collectors.toList());
    }

    private void sendMessage(List<Long> userIds, SmPushTask task, SystemMessage msg) {
        SystemMessageVO msgInfo = new SystemMessageVO();
        msgInfo.setId(task.getMessageId());
        msgInfo.setSeqNo(task.getSeqNo());
        msgInfo.setType(MessageType.SYSTEM_MESSAGE.code());
        msgInfo.setTitle(msg.getTitle());
        msgInfo.setCoverUrl(msg.getCoverUrl());
        msgInfo.setIntro(msg.getIntro());
        msgInfo.setStatus(MessageStatus.DELIVERED.code());
        msgInfo.setSendTime(new Date());
        IMSystemMessage<SystemMessageVO> sendMessage = new IMSystemMessage<>();
        sendMessage.setRecvIds(userIds);
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(true);
        imClient.sendSystemMessage(sendMessage);

    }
}
