package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.entity.SystemMessage;
import com.bx.implatform.vo.SystemMessageContentVO;
import com.bx.implatform.vo.SystemMessageVO;

import java.util.List;

/**
 * @author: Blue
 * @date: 2024-09-07
 * @version: 1.0
 */
public interface SystemMessageService extends IService<SystemMessage> {

    /**
     * 获取系统消息主体内容
     *
     * @param id 消息id
     * @return
     */
    SystemMessageContentVO getMessageContent(Long id);


    /**
     * 拉取离线消息
     * @param minSeqNo 最小发送序列号
     */
    List<SystemMessageVO> loadOfflineMessage(Long minSeqNo);


    /**
     * 已读消息
     * @param maxSeqNo
     */
    void readedMessage(Long maxSeqNo);

}
