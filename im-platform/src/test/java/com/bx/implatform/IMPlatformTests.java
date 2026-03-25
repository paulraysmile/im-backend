package com.bx.implatform;

import cn.hutool.core.util.IdUtil;
import com.bx.implatform.entity.GroupMessage;
import com.bx.implatform.entity.PrivateMessage;
import com.bx.implatform.entity.User;
import com.bx.implatform.service.GroupMessageCompanyService;
import com.bx.implatform.service.PrivateMessageCompanyService;
import com.bx.implatform.service.UserService;
import jakarta.annotation.Resource;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class IMPlatformTests {

    @Resource
    private PrivateMessageCompanyService privateMessageService;
    @Resource
    private GroupMessageCompanyService groupMessageService;
    @Resource
    private UserService userService;

    @Test
    public void testInsertUser() {
        //User user = new User();
        //user.setUserName("123456789");
        //user.setNickName("Jack");
        //user.setPassword("123456789");
        //user.setCompanyId(1L);
        //userService.save(user);
        boolean exists = userService.lambdaQuery().eq(User::getId, 1L).exists();
        System.out.println("exists:" + exists);
    }

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
