package com.bx.implatform.task.schedule;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bx.implatform.entity.FriendRequest;
import com.bx.implatform.enums.FriendRequestStatus;
import com.bx.implatform.service.FriendRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 好友审核超时任务,将一个月未处理的请求置为超时状态
 * @author Blue
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestExpireTask {

    private final FriendRequestService friendRequestService;

    @Scheduled(fixedRate = 10,timeUnit = TimeUnit.MINUTES)
    public void run() {
        log.info("【定时任务】好友审核超时处理...");
        Date minDate = DateUtils.addMonths(new Date(), -1);
        LambdaUpdateWrapper<FriendRequest> wrapper = Wrappers.lambdaUpdate();
        wrapper.le(FriendRequest::getApplyTime,minDate);
        wrapper.eq(FriendRequest::getStatus, FriendRequestStatus.PENDING.getCode());
        wrapper.set(FriendRequest::getStatus,FriendRequestStatus.EXPIRED.getCode());
        friendRequestService.update(wrapper);
    }

}
