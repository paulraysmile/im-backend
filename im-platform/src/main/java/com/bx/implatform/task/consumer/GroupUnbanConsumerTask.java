package com.bx.implatform.task.consumer;

import com.bx.imclient.IMClient;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.model.IMGroupMessage;
import com.bx.imcommon.model.IMUserInfo;
import com.bx.imcommon.mq.RedisMQConsumer;
import com.bx.imcommon.mq.RedisMQListener;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.dto.GroupUnbanDTO;
import com.bx.implatform.entity.GroupMessage;
import com.bx.implatform.enums.MessageStatus;
import com.bx.implatform.enums.MessageType;
import com.bx.implatform.service.GroupMemberService;
import com.bx.implatform.service.GroupMessageCompanyService;
import com.bx.implatform.util.BeanUtils;
import com.bx.implatform.vo.GroupMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@RedisMQListener(queue = RedisKey.IM_QUEUE_GROUP_UNBAN)
public class GroupUnbanConsumerTask extends RedisMQConsumer<GroupUnbanDTO> {

    private final IMClient imClient;

    private final GroupMessageCompanyService groupMessageService;

    private final GroupMemberService groupMemberService;

    @Override
    public void onMessage(GroupUnbanDTO dto) {
        log.info("群聊解除封禁处理,群id:{}",dto.getId());
        // 群聊成员列表
        List<Long> userIds = groupMemberService.findUserIdsByGroupId(dto.getId());
        // 保存消息
        GroupMessage msg = new GroupMessage();
        msg.setCompanyId(dto.getCompanyId());
        msg.setGroupId(dto.getId());
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
}
