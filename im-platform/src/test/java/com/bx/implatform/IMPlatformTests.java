package com.bx.implatform;

import cn.hutool.core.util.IdUtil;
import com.bx.implatform.entity.GroupMessage;
import com.bx.implatform.entity.PrivateMessage;
import com.bx.implatform.service.GroupMessageService;
import com.bx.implatform.service.PrivateMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@Slf4j
@SpringBootTest
public class IMPlatformTests {

    @Resource
    private PrivateMessageService privateMessageService;
    @Resource
    private GroupMessageService groupMessageService;

    @Test
    public void testInsertPrivateMessage() {
        PrivateMessage message = new PrivateMessage();
        message.setCompanyId(2L);
        message.setTmpId(IdUtil.fastSimpleUUID());
        message.setSendId(1L);
        message.setRecvId(1L);
        message.setContent("个人消息");
        message.setType(0);
        message.setStatus(0);
        message.setSendTime(new Date());
        privateMessageService.save(message);
    }

    @Test
    public void testInsertGroupMessage() {
        GroupMessage message = new GroupMessage();
        message.setCompanyId(2L);
        message.setTmpId(IdUtil.fastSimpleUUID());
        message.setGroupId(1L);
        message.setSendId(1L);
        message.setSendNickName("测试用户");
        message.setContent("群消息");
        message.setType(0);
        message.setStatus(0);
        message.setSendTime(new Date());
        groupMessageService.save(message);
    }

}
