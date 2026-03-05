package com.bx.implatform.task.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bx.implatform.annotation.RedisLock;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.User;
import com.bx.implatform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 解除到达条件用户的封禁状态 每小时执行一次
 *
 * @author Blue
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserUnbanTask {

    private final UserService userService;
    private final int batchSize = 1000;

    @RedisLock(prefixKey = RedisKey.IM_LOCK_USER_UNBAN_TASK)
    @Scheduled(cron = "0 15 * * * ?")
    public void run() {
        log.info("【定时任务】解除用户封禁状态处理...");
        // 由于任务一个小时才执行一次，需要提前一个小时解除封禁
        Date unbanTime = DateUtils.addHours(new Date(), 1);
        while (true) {
            // 查询需要解封的用户
            List<User> users = loadBannedUsers(unbanTime, batchSize);
            if (users.isEmpty()) {
                break;
            }
            // 批量更新解封状态
            List<Long> userIds = users.stream().map(User::getId).toList();
            LambdaUpdateWrapper<User> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.in(User::getId, userIds);
            updateWrapper.set(User::getIsBanned, false);
            updateWrapper.set(User::getUnbanTime, null);
            updateWrapper.set(User::getReason, Strings.EMPTY);
            userService.update(updateWrapper);
            log.info("批量解封用户成功，本次解封数量: {}", users.size());
            // 如果查询结果少于批次大小，说明已经处理完所有数据
            if (users.size() < batchSize) {
                break;
            }
        }
    }

    private List<User> loadBannedUsers(Date unbanTime, int size) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getIsBanned, true);
        wrapper.le(User::getUnbanTime, unbanTime);
        wrapper.orderByAsc(User::getId);
        wrapper.last("limit " + size);
        return userService.list(wrapper);
    }
}
