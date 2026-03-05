package com.bx.implatform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.bx.imcommon.enums.IMTerminalType;
import com.bx.imcommon.util.JwtUtil;
import com.bx.implatform.config.props.JwtProperties;
import com.bx.implatform.contant.Constant;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.enums.QrLoginStatus;
import com.bx.implatform.exception.GlobalException;
import com.bx.implatform.service.QrLoginService;
import com.bx.implatform.session.SessionContext;
import com.bx.implatform.session.UserSession;
import com.bx.implatform.vo.LoginVO;
import com.bx.implatform.vo.QrLoginStatusVO;
import com.bx.implatform.vo.QrLoginVO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrLoginServiceImpl implements QrLoginService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public QrLoginVO generateQrCode() {
        // 生成唯一二维码标识
        String qrCode = UUID.randomUUID().toString().replace("-", "");
        // 创建二维码内容
        String qrContent = String.format("{\"qrCode\":\"%s\",\"type\":\"login\",\"timestamp\":%d}", qrCode,
            System.currentTimeMillis());
        try {
            // 生成二维码图片
            String qrImage = generateQrCodeImage(qrContent);
            // 存储登录状态到Redis
            Map<String, Object> loginData = new HashMap<>();
            loginData.put("status", QrLoginStatus.WAITING.name());
            loginData.put("timestamp", System.currentTimeMillis());
            String key = StrUtil.join(":", RedisKey.IM_LOGIN_QRCODE, qrCode);
            redisTemplate.opsForValue().set(key, loginData, Constant.QR_LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
            QrLoginVO vo = new QrLoginVO();
            vo.setQrCode(qrCode);
            vo.setQrImage(qrImage);
            vo.setExpiresIn(Constant.QR_LOGIN_EXPIRE_MINUTES * 60);
            log.info("生成扫码登录二维码: {}", qrCode);
            return vo;
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            throw new GlobalException("生成二维码失败");
        }
    }

    @Override
    public QrLoginStatusVO getLoginStatus(String qrCode) {
        QrLoginStatusVO vo = new QrLoginStatusVO();
        String key = StrUtil.join(":", RedisKey.IM_LOGIN_QRCODE, qrCode);
        Object data = redisTemplate.opsForValue().get(key);
        if (Objects.isNull(data)) {
            vo.setStatus(QrLoginStatus.EXPIRED.name());
            vo.setMessage("二维码已过期");
            return vo;
        }
        Map<String, Object> loginData = (Map<String, Object>)data;
        String status = (String)loginData.get("status");
        vo.setStatus(status);
        switch (QrLoginStatus.valueOf(status)) {
            case WAITING:
                vo.setMessage("等待扫码");
                break;
            case SCANNED:
                vo.setMessage("已扫码，等待确认");
                break;
            case CONFIRMED:
                vo.setMessage("登录成功");
                // 返回登录信息
                LoginVO loginInfo = (LoginVO)loginData.get("loginInfo");
                vo.setLoginInfo(loginInfo);
                // 清除Redis中的登录数据
                redisTemplate.delete(key);
                break;
            case EXPIRED:
                vo.setMessage("二维码已过期");
                break;
        }
        return vo;
    }

    @Override
    public void scanQrCode(String qrCode) {
        String key = StrUtil.join(":", RedisKey.IM_LOGIN_QRCODE, qrCode);
        Object data = redisTemplate.opsForValue().get(key);
        if (Objects.isNull(data)) {
            throw new GlobalException("二维码已过期");
        }
        Map<String, Object> loginData = (Map<String, Object>)data;
        String status = (String)loginData.get("status");
        if (!QrLoginStatus.WAITING.name().equals(status)) {
            throw new GlobalException("二维码状态异常");
        }
        // 更新状态为已扫码
        loginData.put("status", QrLoginStatus.SCANNED.name());
        redisTemplate.opsForValue().set(key, loginData, Constant.QR_LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("用户扫描二维码: {}", qrCode);
    }

    @Override
    public LoginVO confirmQrLogin(String qrCode) {
        String key = StrUtil.join(":", RedisKey.IM_LOGIN_QRCODE, qrCode);
        Object data = redisTemplate.opsForValue().get(key);
        if (Objects.isNull(data)) {
            throw new GlobalException("二维码已过期");
        }
        Map<String, Object> loginData = (Map<String, Object>)data;
        String status = (String)loginData.get("status");
        if (!QrLoginStatus.SCANNED.name().equals(status)) {
            throw new GlobalException("二维码状态异常");
        }
        // 获取当前登录用户信息
        UserSession session = SessionContext.getSession();
        // 生成登录token
        UserSession newSession = new UserSession();
        newSession.setUserId(session.getUserId());
        newSession.setUserName(session.getUserName());
        newSession.setNickName(session.getNickName());
        newSession.setTerminal(IMTerminalType.WEB.code()); // Web终端
        String strJson = JSON.toJSONString(newSession);
        String accessToken = JwtUtil.sign(session.getUserId(), strJson, jwtProperties.getAccessTokenExpireIn(),
            jwtProperties.getAccessTokenSecret());
        String refreshToken = JwtUtil.sign(session.getUserId(), strJson, jwtProperties.getRefreshTokenExpireIn(),
            jwtProperties.getRefreshTokenSecret());
        // 登陆信息
        LoginVO loginInfo = new LoginVO();
        loginInfo.setAccessToken(accessToken);
        loginInfo.setAccessTokenExpiresIn(jwtProperties.getAccessTokenExpireIn());
        loginInfo.setRefreshToken(refreshToken);
        loginInfo.setRefreshTokenExpiresIn(jwtProperties.getRefreshTokenExpireIn());
        // 更新Redis状态
        loginData.put("status", QrLoginStatus.CONFIRMED.name());
        loginData.put("loginInfo", loginInfo);
        redisTemplate.opsForValue().set(key, loginData, Constant.QR_LOGIN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("用户确认扫码登录: userId={}, qrCode={}", session.getUserId(), qrCode);
        return loginInfo;
    }

    @Override
    public void cancelQrLogin(String qrCode) {
        String key = StrUtil.join(":", RedisKey.IM_LOGIN_QRCODE, qrCode);
        redisTemplate.delete(key);
        log.info("取消扫码登录: {}", qrCode);
    }

    /**
     * 生成二维码图片
     */
    private String generateQrCodeImage(String content) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
