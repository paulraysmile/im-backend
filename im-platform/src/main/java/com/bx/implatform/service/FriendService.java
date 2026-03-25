package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.FriendDndDTO;
import com.bx.implatform.dto.FriendRemarkDTO;
import com.bx.implatform.dto.FriendTopDTO;
import com.bx.implatform.entity.Friend;
import com.bx.implatform.vo.FriendVO;

import java.util.List;
import java.util.Map;

public interface FriendService extends IService<Friend> {

    /**
     * 判断是否为好友
     *
     * @param userId 用户id
     * @param friendId 好友id
     * @return true/false
     */
    Boolean isFriend(Long userId, Long friendId);

    /**
     * 查询用户的所有好友,包括已删除的
     *
     * @return 好友列表
     */
    List<Friend> findAllFriends();

    /**
     * 查询用户的所有好友
     *
     * @param friendIds 好友id
     * @return 好友列表
     */
    List<Friend> findByFriendIds(List<Long> friendIds);

    /**
     * 查询当前用户的所有好友
     *
     * @return 好友列表
     */
    List<FriendVO> findFriends();

    /**
     * 添加好友，互相建立好友关系
     *
     * @param userId      用户id
     * @param friendId    好友的用户id
     * @param applyRemark 申请备注
     */
    void addFriend(Long userId, Long friendId, String applyRemark);

    /**
     * 删除好友，双方都会解除好友关系
     *
     * @param friendId 好友的用户id
     */
    void delFriend(Long friendId);

    /**
     * 查询指定的某个好友信息
     *
     * @param friendId 好友的用户id
     * @return 好友信息
     */
    FriendVO findFriend(Long friendId);

    /**
     * 绑定好友关系
     *
     * @param userId   好友的id
     * @param friendId 好友的用户id
     * @return 好友信息
     */
    void bindFriend(Long userId, Long friendId);

    /**
     * 修改好友备注
     *
     * @param dto dto
     * @return 好友信息
     */
    FriendVO modifyRemark(FriendRemarkDTO dto);

    /**
     * 查询用户昵称
     *
     * @param friendIds 用户id列表
     * @return 昵称
     */
    Map<Long, String> loadRemark(List<Long> friendIds);

    /**
     * 推送在线状态给所有好友
     *
     * @param userId   用户id
     * @param terminal 终端类型
     */
    void sendOnlineStatus(Long userId, Integer terminal);

    /**
     * 初始化免打扰缓存
     */
    void initDndCache(Long userId);

    /**
     * 设置好友免打扰状态
     *
     * @param dto
     */
    void setDnd(FriendDndDTO dto);

    /**
     * 是否开启消息免打扰
     *
     * @param userId   用户id
     * @param friendId 好友id
     */
    Boolean isDnd(Long userId, Long friendId);

    /**
     * 设置好友会话置顶状态
     *
     * @param dto
     */
    void setTop(FriendTopDTO dto);
}
