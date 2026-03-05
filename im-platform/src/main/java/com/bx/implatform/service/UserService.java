package com.bx.implatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bx.implatform.dto.*;
import com.bx.implatform.entity.User;
import com.bx.implatform.enums.RealnameAuthStatus;
import com.bx.implatform.vo.LoginVO;
import com.bx.implatform.vo.UserVO;

import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户登录
     *
     * @param dto 登录dto
     * @return 登录token
     */
    LoginVO login(LoginDTO dto);


    /**
     * 修改用户密码
     *
     * @param dto 修改密码dto
     */
    void modifyPassword(ModifyPwdDTO dto);

    /**
     * 重置用户密码
     * @param dto 重置密码dto
     */
    void resetPassword(ResetPwdDTO dto);

    /**
     * 用refreshToken换取新 token
     *
     * @param refreshToken 刷新token
     * @return 登录token
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 用户注册
     *
     * @param dto 注册dto
     */
    void register(RegisterDTO dto);


    /**
     * 用户注销
     *
     */
    void unregister();

    /**
     * 根据登录名查询用户
     *
     * @param loginName 登录名
     * @return 用户信息
     */
    User findUserByLoginName(String loginName);


    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User findUserByUserName(String username);


    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User findUserByPhone(String phone);


    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    User findUserByEmail(String email);

    /**
     * 是否存在手机用户
     *
     * @param phone 手机号
     * @return
     */
    Boolean isExistPhone(String phone);

    /**
     * 是否存在邮箱用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    Boolean isExistEmail(String email);

    /**
     * 更新用户信息，好友昵称和群聊昵称等冗余信息也会更新
     *
     * @param vo 用户信息vo
     */
    void update(UserVO vo);

    /**
     * 根据用户昵id查询用户以及在线状态
     *
     * @param id 用户id
     * @return 用户信息
     */
    UserVO findUserById(Long id);

    /**
     * 查询当前用户的信息
     *
     * @return 用户信息
     */
    UserVO findSelfInfo();
    /**
     * 根据用户昵称查询用户，最多返回20条数据
     *
     * @param name 用户名或昵称
     * @return 用户列表
     */
    List<UserVO> findUserByName(String name);

    /**
     * 查询用户，最多返回20条数据
     *
     * @param name 用户名/昵称/手机号/邮箱
     * @return 用户列表
     */
    List<UserVO> search(String name);

    /**
     * 上报用户cid
     * @param cid 用户cid
     */
    void reportCid(String cid);

    /**
     *  清理用户cid
     */
    void removeCid();

    /**
     * 开启/关闭好友验证
     * @param enabled
     */
    void setManualApprove(Boolean enabled);

    /**
     * 开启/关闭新消息提醒
     * @param enabled
     */
    void setAudioTip(Boolean enabled);

    /**
     * 绑定手机号
     * @param dto dto
     */
    void bindPhone(BindPhoneDTO dto);

    /**
     * 绑定邮箱
     * @param dto dto
     */
    void bindEmail(BindEmailDTO dto);

    /**
     * 更新实名认证状态
     *
     * @param status
     */
    void updateAuthStatus(RealnameAuthStatus status);


}
