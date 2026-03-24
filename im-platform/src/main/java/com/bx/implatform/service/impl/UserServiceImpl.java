package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bx.imclient.IMClient;
import com.bx.imcommon.model.IMSystemMessage;
import com.bx.imcommon.util.JwtUtil;
import com.bx.implatform.config.props.JwtProperties;
import com.bx.implatform.config.props.NotifyProperties;
import com.bx.implatform.config.props.RegistrationProperties;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.dto.*;
import com.bx.implatform.entity.Friend;
import com.bx.implatform.entity.GroupMember;
import com.bx.implatform.entity.User;
import com.bx.implatform.enums.*;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.mapper.UserMapper;
import com.bx.implatform.service.*;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.util.*;
import com.bx.implatform.vo.LoginVO;
import com.bx.implatform.vo.SystemMessageVO;
import com.bx.implatform.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final UserBlacklistService userBlacklistService;
    private final FriendService friendService;
    private final JwtProperties jwtProps;
    private final NotifyProperties notifyProps;
    private final RegistrationProperties registrationProps;
    private final IMClient imClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaService captchaService;
    private final SensitiveFilterUtil sensitiveFilterUtil;
    private final HttpServletRequest request;
    private final CompanyService companyService;

    @Override
    public LoginVO login(LoginDTO dto) {
        Long companyId = companyService.selectIdByInviteCode(dto.getInviteCode());
        if (companyId == null) {
            throw new GlobalException("邀请码无效");
        }
        User user = this.findUserByLoginName(dto.getUserName(), companyId);
        if (Objects.isNull(user)) {
            log.warn("用户不存在,name:{},companyId:{}", dto.getUserName(), companyId);
            throw new GlobalException("用户不存在");
        }
        if (user.getStatus().equals(UserStatus.UN_REG.getValue())) {
            throw new GlobalException("您的账号已注销");
        }
        // 检查是否已经被封禁
        checkIsBan(user);
        // 密码校验
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new GlobalException(ResultCode.PASSWOR_ERROR);
        }
        // 更新用户登录ip、时间
        this.lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginIp, JakartaServletUtil.getClientIP(request))
                .set(User::getLastLoginTime, new Date())
                .update();
        // 初始化免打扰缓存
        friendService.initDndCache(user.getId());
        groupService.initDndCache(user.getId());
        // 生成token
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setTerminal(dto.getTerminal());
        session.setUserName(user.getUserName());
        session.setNickName(user.getNickName());
        session.setCompanyId(companyId);
        String strJson = JSON.toJSONString(session);
        String accessToken = JwtUtil.sign(user.getId(), strJson, jwtProps.getAccessTokenExpireIn(), jwtProps.getAccessTokenSecret());
        String refreshToken = JwtUtil.sign(user.getId(), strJson, jwtProps.getRefreshTokenExpireIn(), jwtProps.getRefreshTokenSecret());
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setAccessTokenExpiresIn(jwtProps.getAccessTokenExpireIn());
        vo.setRefreshToken(refreshToken);
        vo.setRefreshTokenExpiresIn(jwtProps.getRefreshTokenExpireIn());
        return vo;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        //验证 token
        if (!JwtUtil.checkSign(refreshToken, jwtProps.getRefreshTokenSecret())) {
            throw new GlobalException("您的登录信息已过期，请重新登录");
        }
        String strJson = JwtUtil.getInfo(refreshToken);
        Long userId = JwtUtil.getUserId(refreshToken);
        User user = this.getById(userId);
        if (Objects.isNull(user)) {
            log.warn("用户不存在,id:{}", userId);
            throw new GlobalException("用户不存在");
        }
        // 检查是否已经被封禁
        checkIsBan(user);
        // 更新用户登录ip、时间
        this.lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginIp, JakartaServletUtil.getClientIP(request))
                .set(User::getLastLoginTime, new Date())
                .update();

        String accessToken = JwtUtil.sign(userId, strJson, jwtProps.getAccessTokenExpireIn(), jwtProps.getAccessTokenSecret());
        String newRefreshToken = JwtUtil.sign(userId, strJson, jwtProps.getRefreshTokenExpireIn(), jwtProps.getRefreshTokenSecret());
        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setAccessTokenExpiresIn(jwtProps.getAccessTokenExpireIn());
        vo.setRefreshToken(newRefreshToken);
        vo.setRefreshTokenExpiresIn(jwtProps.getRefreshTokenExpireIn());
        return vo;
    }

    @Override
    public void register(RegisterDTO dto) {
        String userName = dto.getUserName();
        // 校验用户名
        if (RegexUtil.isPhone(userName) || RegexUtil.isEmail(userName)) {
            throw new GlobalException("用户名不合法");
        }
        if (!userName.equals(sensitiveFilterUtil.filter(userName))) {
            throw new GlobalException("用户名包含敏感字符");
        }
        String nickName = dto.getNickName();
        if (StrUtil.isNotBlank(nickName) && !nickName.equals(sensitiveFilterUtil.filter(nickName))) {
            throw new GlobalException("昵称包含敏感字符");
        }
        Long companyId = companyService.selectIdByInviteCode(dto.getInviteCode());
        if (companyId == null) {
            throw new GlobalException("邀请码无效");
        }
        if (isExistUsername(userName, companyId)) {
            throw new GlobalException(ResultCode.USERNAME_ALREADY_REGISTER);
        }
        User user = new User();
        // 手机、验证码校验
        if (RegisterMode.PHONE.getCode().equals(dto.getMode())) {
            if (!RegexUtil.isPhone(dto.getPhone())) {
                throw new GlobalException("手机号格式不合法");
            }
            if (isExistPhone(dto.getPhone(), companyId)) {
                throw new GlobalException("该手机号已被注册");
            }
            if (!captchaService.vertify(CaptchaType.SMS, dto.getPhone(), dto.getCode())) {
                throw new GlobalException("验证码错误");
            }
            user.setPhone(dto.getPhone());
        }
        // 邮箱、验证码校验
        if (RegisterMode.EMAIL.getCode().equals(dto.getMode())) {
            if (!RegexUtil.isEmail(dto.getEmail())) {
                throw new GlobalException("邮箱格式不合法");
            }
            if (isExistEmail(dto.getEmail(), companyId)) {
                throw new GlobalException("该邮箱已被注册");
            }
            if (!captchaService.vertify(CaptchaType.MAIL, dto.getEmail(), dto.getCode())) {
                throw new GlobalException("验证码错误");
            }
            user.setEmail(dto.getEmail());
        }
        // 保存用户信息
        String ip = JakartaServletUtil.getClientIP(request);
        if (StrUtil.isEmpty(ip)) {
            throw new GlobalException("您的IP地址异常,无法注册");
        }
        // IP注册限制检查
        checkIpRegisterLimit(ip);
        user.setLastLoginIp(ip);
        user.setLastLoginTime(new Date());
        user.setUserName(userName);
        user.setNickName(nickName);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 昵称默认和用户名保持一致
        if (StrUtil.isEmpty(nickName)) {
            dto.setNickName(userName);
        }
        user.setCompanyId(companyId);
        try {
            this.save(user);
        } catch (Exception e) {
            log.error("[register] username:{} companyId:{}", e);
            throw new GlobalException("重复注册");
        }
        log.info("[register] companyId:{} userId:{} username:{} nickName:{} IP:{}", companyId, user.getId(), userName, nickName, ip);
    }

    @Transactional
    @Override
    public void unregister() {
        UserSession session = SessionContext.getSession();
        // 修改用户状态
        User user = this.getById(session.getUserId());
        if (user.getType().equals(UserType.OPEN_ACCOUNT.getValue())) {
            throw new GlobalException("您当前使用的是公开体验账号,不允许注销");
        }
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getId, user.getId());
        wrapper.set(User::getCid, Strings.EMPTY);
        wrapper.set(User::getStatus, UserStatus.UN_REG.getValue());
        // 释放手机号和邮箱，否则会无法重新注册
        wrapper.set(User::getPhone, null);
        wrapper.set(User::getEmail, null);
        this.update(wrapper);
        // 清理redis中的cid
        String key1 = StrUtil.join(":", RedisKey.IM_USER_CID, user.getId());
        redisTemplate.delete(key1);
        // 推送消息让用户下线
        SystemMessageVO msgInfo = new SystemMessageVO();
        msgInfo.setType(MessageType.USER_UNREG.code());
        msgInfo.setSendTime(new Date());
        IMSystemMessage<SystemMessageVO> sendMessage = new IMSystemMessage<>();
        sendMessage.setRecvIds(Collections.singletonList(session.getUserId()));
        sendMessage.setData(msgInfo);
        sendMessage.setSendResult(false);
        imClient.sendSystemMessage(sendMessage);
    }

    @Override
    public void modifyPassword(ModifyPwdDTO dto) {
        UserSession session = SessionContext.getSession();
        User user = this.getById(session.getUserId());
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new GlobalException("原密码不正确");
        }
        if (user.getType().equals(UserType.OPEN_ACCOUNT.getValue())) {
            throw new GlobalException("您当前使用的是公开体验账号,不允许修改密码");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
        log.info("用户修改密码，用户id:{},用户名:{},昵称:{}", user.getId(), user.getUserName(), user.getNickName());
    }

    @Override
    public void resetPassword(ResetPwdDTO dto) {
        Long companyId = companyService.selectIdByInviteCode(dto.getInviteCode());
        if (companyId == null) {
            throw new GlobalException("邀请码无效");
        }
        if (RegisterMode.PHONE.getCode().equals(dto.getMode())) {
            // 手机验证码校验
            resetPwdByPhone(dto.getPhone(), dto.getCode(), dto.getPassword(), companyId);
        } else if (RegisterMode.EMAIL.getCode().equals(dto.getMode())) {
            // 邮箱验证码校验
            resetPwdByEmail(dto.getEmail(), dto.getCode(), dto.getPassword(), companyId);
        } else {
            throw new GlobalException("不支持该密码重置方式");
        }
    }

    private void resetPwdByPhone(String phone, String code, String password, Long companyId) {
        // 手机验证码校验
        if (!captchaService.vertify(CaptchaType.SMS, phone, code)) {
            throw new GlobalException("验证码错误");
        }
        User user = findUserByPhone(phone, companyId);
        if (Objects.isNull(user)) {
            throw new GlobalException("该手机号未注册");
        }
        user.setPassword(passwordEncoder.encode(password));
        this.updateById(user);
        log.info("通过手机重置用户密码，用户id:{},用户名:{}", user.getId(), user.getUserName());
    }

    private void resetPwdByEmail(String email, String code, String password, Long companyId) {
        // 手机验证码校验
        if (!captchaService.vertify(CaptchaType.MAIL, email, code)) {
            throw new GlobalException("验证码错误");
        }
        User user = findUserByEmail(email, companyId);
        if (Objects.isNull(user)) {
            throw new GlobalException("该邮箱未注册");
        }
        user.setPassword(passwordEncoder.encode(password));
        this.updateById(user);
        log.info("通过邮箱用户密码，用户id:{},用户名:{}", user.getId(), user.getUserName());
    }

    @Override
    public User findUserByUserName(String username, Long companyId) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getUserName, username);
        queryWrapper.eq(User::getCompanyId, companyId);
        return this.getOne(queryWrapper);
    }

    @Override
    public User findUserByPhone(String phone, Long companyId) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getPhone, phone);
        queryWrapper.eq(User::getCompanyId, companyId);
        return this.getOne(queryWrapper);
    }

    @Override
    public User findUserByEmail(String email, Long companyId) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getEmail, email);
        queryWrapper.eq(User::getCompanyId, companyId);
        return this.getOne(queryWrapper);
    }

    @Override
    public User findUserByLoginName(String loginName, Long companyId) {
        // 优先用户名登陆
        User user = findUserByUserName(loginName, companyId);
        // 手机号登陆
        if (Objects.isNull(user) && RegexUtil.isPhone(loginName)) {
            user = findUserByPhone(loginName, companyId);
        }
        // 邮箱登陆
        if (Objects.isNull(user) && RegexUtil.isEmail(loginName)) {
            user = findUserByEmail(loginName, companyId);
        }
        return user;
    }

    @Override
    public boolean isExistUsername(String username, Long companyId) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getUserName, username);
        queryWrapper.eq(User::getCompanyId, companyId);
        return this.exists(queryWrapper);
    }

    @Override
    public boolean isExistPhone(String phone, Long companyId) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getPhone, phone);
        queryWrapper.eq(User::getCompanyId, companyId);
        return this.exists(queryWrapper);
    }

    @Override
    public boolean isExistEmail(String email, Long companyId) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getEmail, email);
        queryWrapper.eq(User::getCompanyId, companyId);
        return this.exists(queryWrapper);
    }

    @Transactional
    @Override
    public void update(UserVO vo) {
        UserSession session = SessionContext.getSession();
        if (!vo.getNickName().equals(sensitiveFilterUtil.filter(vo.getNickName()))) {
            throw new GlobalException("昵称包含敏感字符");
        }
        if (!session.getUserId().equals(vo.getId())) {
            throw new GlobalException("不允许修改其他用户的信息!");
        }
        User user = this.getById(vo.getId());
        if (Objects.isNull(user)) {
            log.warn("用户不存在,id:{}", vo.getId());
            throw new GlobalException("用户不存在");
        }
        if (user.getType().equals(UserType.OPEN_ACCOUNT.getValue())) {
            throw new GlobalException("您当前使用的是公开体验账号,不允许修改用户资料");
        }
        if (!user.getNickName().equals(vo.getNickName()) || !user.getHeadImageThumb().equals(vo.getHeadImageThumb())) {
            // 更新好友昵称和头像
            LambdaUpdateWrapper<Friend> wrapper1 = Wrappers.lambdaUpdate();
            wrapper1.eq(Friend::getFriendId, session.getUserId());
            wrapper1.set(Friend::getFriendNickName, vo.getNickName());
            wrapper1.set(Friend::getFriendHeadImage, vo.getHeadImageThumb());
            friendService.update(wrapper1);
            // 更新群聊中的昵称和头像
            LambdaUpdateWrapper<GroupMember> wrapper2 = Wrappers.lambdaUpdate();
            wrapper2.eq(GroupMember::getUserId, session.getUserId());
            wrapper2.set(GroupMember::getHeadImage, vo.getHeadImageThumb());
            wrapper2.set(GroupMember::getUserNickName, vo.getNickName());
            groupMemberService.update(wrapper2);
        }
        // 更新用户信息
        user.setNickName(vo.getNickName());
        user.setSex(vo.getSex());
        user.setSignature(vo.getSignature());
        user.setHeadImage(vo.getHeadImage());
        user.setHeadImageThumb(vo.getHeadImageThumb());
        this.updateById(user);
        log.info("用户信息更新，用户:{}}", user);
    }

    @Override
    public UserVO findUserById(Long id) {
        UserSession session = SessionContext.getSession();
        User user = this.getById(id);
        if (Objects.isNull(user)) {
            log.warn("用户不存在,id:{}", id);
            throw new GlobalException("用户不存在");
        }
        UserVO vo = convert(user);
        vo.setOnline(imClient.isOnline(id));
        vo.setIsInBlacklist(userBlacklistService.isInBlacklist(session.getUserId(), id));
        return vo;
    }

    @Override
    public UserVO findSelfInfo() {
        UserSession session = SessionContext.getSession();
        User user = this.getById(session.getUserId());
        UserVO vo = BeanUtils.copyProperties(user, UserVO.class);
        vo.setIsAudioTip((user.getAudioTip() & 1 << session.getTerminal()) > 0);
        return vo;
    }

    @Override
    public List<UserVO> findUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getIsBanned, false);
        wrapper.eq(User::getStatus, UserStatus.NORMAL.getValue());
        wrapper.eq(User::getUserName, username);
        List<User> users = this.list(wrapper);
        return convert(users);
    }

    @Override
    public List<UserVO> search(String name) {
        Long companyId = SessionContext.getSession().getCompanyId();
        if (RegexUtil.isPhone(name)) {
            // 查询手机号
            User user = findUserByPhone(name, companyId);
            if (!Objects.isNull(user)) {
                return convert(List.of(user));
            }
        } else if (RegexUtil.isEmail(name)) {
            // 查询邮箱
            User user = findUserByEmail(name, companyId);
            if (!Objects.isNull(user)) {
                return convert(List.of(user));
            }
        } else {
            // 查询用户名
            return findUserByUsername(name);
        }
        return Lists.newArrayList();
    }

    @Transactional
    @Override
    public void reportCid(String cid) {
        UserSession session = SessionContext.getSession();
        // 清理该设备以前登录过的cid
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getCid, cid);
        wrapper.ne(User::getId, session.getUserId());
        List<User> users = this.list(wrapper);
        users.forEach(user -> {
            // 清理redis中的cid
            String key1 = StrUtil.join(":", RedisKey.IM_USER_CID, user.getId());
            redisTemplate.delete(key1);
            // 清空通知会话信息
            String key2 = StrUtil.join(":", RedisKey.IM_NOTIFY_OFFLINE_SESSION, user.getId());
            redisTemplate.delete(key2);
            user.setCid(Strings.EMPTY);
            this.updateById(user);
        });
        // 保存当前用户的cid
        User user = this.getById(session.getUserId());
        user.setCid(cid);
        this.updateById(user);
        // 缓存cid到redis
        String key = StrUtil.join(":", RedisKey.IM_USER_CID, user.getId());
        redisTemplate.opsForValue().set(key, cid, notifyProps.getActiveDays(), TimeUnit.DAYS);
        // 清空通知会话信息
        String key2 = StrUtil.join(":", RedisKey.IM_NOTIFY_OFFLINE_SESSION, user.getId());
        redisTemplate.delete(key2);
    }

    @Transactional
    @Override
    public void removeCid() {
        UserSession session = SessionContext.getSession();
        User user = this.getById(session.getUserId());
        user.setCid(Strings.EMPTY);
        this.updateById(user);
        // 清理redis中的cid
        String key1 = StrUtil.join(":", RedisKey.IM_USER_CID, user.getId());
        redisTemplate.delete(key1);
    }

    @Override
    public void setManualApprove(Boolean enabled) {
        UserSession session = SessionContext.getSession();
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getId, session.getUserId());
        wrapper.set(User::getIsManualApprove, enabled);
        this.update(wrapper);
    }

    @Transactional
    @Override
    public void setAudioTip(Boolean enabled) {
        UserSession session = SessionContext.getSession();
        User user = this.getById(session.getUserId());
        if (enabled) {
            int mask = 1 << session.getTerminal();
            user.setAudioTip(user.getAudioTip() | mask);
        } else {
            int mask = ~(1 << session.getTerminal());
            user.setAudioTip(user.getAudioTip() & mask);
        }
        this.updateById(user);
    }

    @Transactional
    @Override
    public void bindPhone(BindPhoneDTO dto) {
        String phone = dto.getPhone();
        if (!RegexUtil.isPhone(phone)) {
            throw new GlobalException("手机号格式不合法");
        }
        UserSession session = SessionContext.getSession();
        User user = this.lambdaQuery()
                .select(User::getId, User::getPhone)
                .eq(User::getId, session.getUserId())
                .one();
        if (StrUtil.isNotEmpty(user.getPhone())) {
            throw new GlobalException("您已绑定了手机号,不可重复绑定");
        }
        if (isExistPhone(phone, session.getCompanyId())) {
            throw new GlobalException("该手机号已被注册");
        }
        if (!captchaService.vertify(CaptchaType.SMS, phone, dto.getCode())) {
            throw new GlobalException("验证码错误");
        }
        this.lambdaUpdate()
                .eq(User::getId, session.getUserId())
                .set(User::getPhone, phone)
                .update();
    }

    @Transactional
    @Override
    public void bindEmail(BindEmailDTO dto) {
        String email = dto.getEmail();
        if (!RegexUtil.isEmail(email)) {
            throw new GlobalException("邮箱格式不合法");
        }
        UserSession session = SessionContext.getSession();
        User user = this.lambdaQuery()
                .select(User::getId, User::getEmail)
                .eq(User::getId, session.getUserId())
                .one();
        if (StrUtil.isNotEmpty(user.getEmail())) {
            throw new GlobalException("您已绑定了邮箱,不可重复绑定");
        }
        if (isExistEmail(email, session.getCompanyId())) {
            throw new GlobalException("该邮箱已被注册");
        }
        if (!captchaService.vertify(CaptchaType.MAIL, email, dto.getCode())) {
            throw new GlobalException("验证码错误");
        }
        this.lambdaUpdate()
                .eq(User::getId, session.getUserId())
                .set(User::getEmail, email)
                .update();
    }

    @Override
    public void updateAuthStatus(RealnameAuthStatus status) {
        UserSession session = SessionContext.getSession();
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getId, session.getUserId());
        wrapper.set(User::getAuthStatus, status.getCode());
        this.update(wrapper);
    }

    List<UserVO> convert(List<User> users) {
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<Long> onlineUserIds = imClient.getOnlineUser(userIds);
        return users.stream().map(u -> {
            UserVO vo = convert(u);
            vo.setOnline(onlineUserIds.contains(u.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private UserVO convert(User user) {
        UserVO vo = BeanUtils.copyProperties(user, UserVO.class);
        // 邮箱和手机号做脱敏处理
        vo.setPhone(DesensitizeUtil.desensitizePhone(vo.getPhone()));
        vo.setEmail(DesensitizeUtil.desensitizeEmail(vo.getEmail()));
        return vo;
    }

    private void checkIsBan(User user) {
        if (user.getIsBanned()) {
            String tip = String.format("您的账号因'%s'被封禁,", user.getReason());
            if (Objects.isNull(user.getUnbanTime())) {
                tip += "封禁时长: 永久";
            } else {
                tip += "恢复时间: " + DateTimeUtils.getFormatDate(user.getUnbanTime(), DateTimeUtils.FULL_DATE_FORMAT);
            }
            throw new GlobalException(tip);
        }
    }

    private void checkIpRegisterLimit(String ip) {
        RegistrationProperties.IpLimit ipLimit = registrationProps.getIpLimit();
        if (!ipLimit.getEnabled() || isLocalAddress(ip)) {
            return;
        }
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getLastLoginIp, ip);
        wrapper.eq(User::getStatus, UserStatus.NORMAL.getValue());
        if (this.count(wrapper) >= ipLimit.getMaxCount()) {
            log.warn("IP注册限制：IP {} 已达到注册上限 {} 个用户", ip, ipLimit.getMaxCount());
            throw new GlobalException(String.format("您已注册%d个账号，已达到上限", ipLimit.getMaxCount()));
        }
    }

    private Boolean isLocalAddress(String ip) {
        return ip.equals("127.0.0.1") || ip.equals("localhost");
    }
}
