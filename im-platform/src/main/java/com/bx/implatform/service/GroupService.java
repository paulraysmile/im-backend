package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.*;
import com.bx.implatform.entity.Group;
import com.bx.implatform.vo.BizTokenVO;
import com.bx.implatform.vo.GroupMemberVO;
import com.bx.implatform.vo.GroupVO;

import java.util.List;

public interface GroupService extends IService<Group> {

    /**
     * 创建新群聊
     *
     * @param vo 群聊信息
     * @return 群聊信息
     **/
    GroupVO createGroup(GroupVO vo);

    /**
     * 修改群聊信息
     *
     * @param vo 群聊信息
     * @return 群聊信息
     **/
    GroupVO modifyGroup(GroupVO vo);

    /**
     * 删除群聊
     *
     * @param groupId 群聊id
     **/
    void deleteGroup(Long groupId);

    /**
     * 退出群聊
     *
     * @param groupId 群聊id
     */
    void quitGroup(Long groupId);


    /**
     * 将用户移出群聊
     * @param dto dto
     */
    void removeGroupMembers(GroupMemberRemoveDTO dto);

    /**
     * 查询当前用户的所有群聊
     *
     * @return 群聊信息列表
     **/
    List<GroupVO> findGroups();

    /**
     * 邀请好友进群
     *
     * @param dto 群id、好友id列表
     **/
    void invite(GroupInviteDTO dto);

    /**
     * 生成二维码token
     *
     * @param groupId 群id
     * @return token
     **/
    BizTokenVO generateQrcodeToken(Long groupId);

    /**
     * 生成名片分享token
     *
     * @param groupId 群id
     * @return token
     **/
    BizTokenVO generateShareCardToken(Long groupId);

    /**
     * 加入群聊
     *
     * @param dto 群id和token
     **/
    GroupVO join(GroupJoinDTO dto);

    /**
     * 根据id查找群聊，并进行缓存
     *
     * @param groupId 群聊id
     * @return 群聊实体
     */
    Group getAndCheckById(Long groupId);

    /**
     * 根据id查找群聊
     *
     * @param groupId 群聊id
     * @return 群聊vo
     */
    GroupVO findById(Long groupId);

    /**
     * 查询群成员
     *
     * @param groupId 群聊id
     * @param version 版本号
     * @return List<GroupMemberVO>
     **/
    List<GroupMemberVO> findGroupMembers(Long groupId,Long version);

    /**
     * 查询在线成员id
     * @param groupId 群聊id
     * @return
     */
    List<Long> findOnlineMemberIds(Long groupId);

    /**
     * 设置群禁言状态
     * @param dto dto
     */
    void  setGroupMuted(GroupMutedDTO dto);


    /**
     * 设置成员禁言状态
     * @param dto dto
     */
    void  setMemberMuted(GroupMemberMutedDTO dto);


    /**
     * 设置群置顶消息
     * @param groupId 群id
     * @param messageId 消息id
     */
    void setTopMessage(Long groupId,Long messageId);

    /**
     * 移除群置顶消息,对所有群成员生效
     * @param groupId 群id
     */
    void removeTopMessage(Long groupId);

    /**
     * 隐藏群置顶消息，仅对自己生效
     * @param groupId 群id
     */
    void hideTopMessage(Long groupId);

    /**
     * 新增管理员
     * @param dto dto
     */
    void addManager(GroupManagerDTO dto);

    /**
     * 移除管理员
     * @param dto dto
     */
    void removeManager(GroupManagerDTO dto);

    /**
     * 开启/关闭免打扰
     * @param dto
     */
    void setDnd(GroupDndDTO dto);

    /**
     * 初始化免打扰缓存
     * @param userId
     */
    void initDndCache(Long userId);


    /**
     * 是否开启消息免打扰
     * @param userId 用户id
     * @param groupId 群id
     */
    Boolean isDnd(Long userId,Long groupId);


    /**
     * 开启/关闭置顶
     * @param dto
     */
    void setTop(GroupTopDTO dto);

    /**
     * 允许/禁止普通成员邀请好友
     * @param dto
     */
    void setAllowInvite(GroupAllowInviteDTO dto);

    /**
     * 允许/禁止普通成员分享名片
     * @param dto
     */
    void setAllowShareCard(GroupAllowShareCardDTO dto);

    /**
     * 允许/禁止普通成员群内互相加好友
     * @param dto
     */
    void setAllowAddOther(GroupAllowAddOtherDTO dto);

}
