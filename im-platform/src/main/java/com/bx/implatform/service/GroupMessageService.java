package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.ChatDeleteDTO;
import com.bx.implatform.dto.GroupMessageRemoveAllDTO;
import com.bx.implatform.dto.GroupMessageRemoveDTO;
import com.bx.implatform.dto.GroupMessageDTO;
import com.bx.implatform.dto.MessageDeleteDTO;
import com.bx.implatform.entity.GroupMessage;
import com.bx.implatform.vo.GroupMessageVO;

import java.util.List;

public interface GroupMessageService extends IService<GroupMessage> {

    /**
     * 发送群聊消息(高并发接口，查询mysql接口都要进行缓存)
     *
     * @param dto 群聊消息
     * @return 群聊id
     */
    GroupMessageVO sendMessage(GroupMessageDTO dto);

    /**
     * 撤回消息
     *
     * @param id 消息id
     */
    GroupMessageVO recallMessage(Long id);


    /**
     * 拉取离线消息，只能拉取最近1个月的消息
     *
     * @param minId 消息起始id
     */
    List<GroupMessageVO> loadOffineMessage(Long minId);

    /**
     * 消息已读,同步其他终端，清空未读数量
     *
     * @param groupId 群聊
     */
    void readedMessage(Long groupId);

    /**
     * 查询群里消息已读用户id列表
     * @param groupId 群里id
     * @param messageId 消息id
     * @return 已读用户id集合
     */
    List<Long> findReadedUsers(Long groupId,Long messageId);

    /**
     * 删除消息
     * @param dto
     */
    void deleteMessage(MessageDeleteDTO dto);

    /**
     * 删除会话
     * @param dto dto
     */
    void deleteChat(ChatDeleteDTO dto);

    /**
     * 移除群聊消息
     * <p>
     * 将指定消息标记为已删除（仅对当前用户生效）
     * </p>
     *
     * @param dto 请求参数
     * @return 最后一条被移除的消息VO
     */
    GroupMessageVO remove(GroupMessageRemoveDTO dto);

    /**
     * 移除全部群聊消息
     * <p>
     * 将当前用户在该群聊中的全部消息标记为已删除
     * </p>
     *
     * @param dto 请求参数
     * @return 最后一条消息VO
     */
    GroupMessageVO removeAll(GroupMessageRemoveAllDTO dto);

}
