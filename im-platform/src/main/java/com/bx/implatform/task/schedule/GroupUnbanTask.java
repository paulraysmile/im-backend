package com.bx.implatform.task.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bx.imclient.IMClient;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMGroupMessage;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.implatform.annotation.RedisLock;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.entity.Group;
import com.bx.implatform.entity.GroupMessage;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.service.GroupMemberService;
import com.bx.implatform.service.GroupMessageService;
import com.bx.implatform.service.GroupService;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.GroupMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 解除到达条件群聊的封禁状态 每小时执行一次
 *
 * @author Blue
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupUnbanTask {

    private final GroupService groupService;
    private final IMClient imClient;
    private final GroupMessageService groupMessageService;
    private final GroupMemberService groupMemberService;

    private final int batchSize = 100;

    @RedisLock(prefixKey = RedisKey.IM_LOCK_GROUP_UNBAN_TASK)
    @Scheduled(cron = "0 45 * * * ?")
    public void run() {
        log.info("【定时任务】解除群聊封禁状态处理...");
        // 由于任务一个小时才执行一次，需要提前一个小时解除封禁
        Date unbanTime = DateUtils.addHours(new Date(), 1);
        while (true) {
            // 查询需要解封的群聊
            List<Group> groups = loadBannedGroups(unbanTime, batchSize);
            if (groups.isEmpty()) {
                break;
            }
            // 批量更新解封状态
            List<Long> groupIds = groups.stream().map(Group::getId).toList();
            LambdaUpdateWrapper<Group> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.in(Group::getId, groupIds);
            updateWrapper.set(Group::getIsBanned, false);
            updateWrapper.set(Group::getUnbanTime, null);
            updateWrapper.set(Group::getReason, Strings.EMPTY);
            groupService.update(updateWrapper);
            for (Group group : groups) {
                // 群聊成员列表
                List<Long> userIds = groupMemberService.findUserIdsByGroupId(group.getId());
                // 保存消息
                GroupMessage msg = new GroupMessage();
                msg.setGroupId(group.getId());
                msg.setContent("已解除封禁");
                msg.setSendId(Constant.SYS_USER_ID);
                msg.setSendTime(new Date());
                msg.setStatus(MessageStatus.PENDING.code());
                msg.setSendNickName("系统");
                msg.setType(MessageType.TIP_TEXT.code());
                groupMessageService.save(msg);
                // 推送提示语到群聊中
                GroupMessageVO msgInfo = BeanUtils.copyProperties(msg, GroupMessageVO.class);
                IMGroupMessage<GroupMessageVO> sendMessage = new IMGroupMessage<>();
                sendMessage.setSender(new IMUserInfo(Constant.SYS_USER_ID, IMTerminalType.UNKNOW.code()));
                sendMessage.setRecvIds(userIds);
                sendMessage.setSendResult(true);
                sendMessage.setSendToSelf(false);
                sendMessage.setData(msgInfo);
                imClient.sendGroupMessage(sendMessage);
            }
            log.info("批量解封群聊成功，本次解封数量: {}", groups.size());
            // 如果查询结果少于批次大小，说明已经处理完所有数据
            if (groups.size() < batchSize) {
                break;
            }
        }
    }

    private List<Group> loadBannedGroups(Date unbanTime, int size) {
        LambdaQueryWrapper<Group> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Group::getIsBanned, true);
        wrapper.le(Group::getUnbanTime, unbanTime);
        wrapper.orderByAsc(Group::getId);
        wrapper.last("limit " + size);
        return groupService.list(wrapper);
    }
}
