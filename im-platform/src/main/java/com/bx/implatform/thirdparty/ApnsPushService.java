package com.bx.implatform.thirdparty;

import cn.hutool.core.util.StrUtil;
import com.bx.implatform.config.props.ApnsProperties;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.concurrent.*;

/**
 * APNs 直连推送服务
 * <p>
 * 使用 Apple .p8 密钥向苹果设备推送离线通知，用于 iOS 客户端未使用个推时
 * </p>
 *
 * @author im-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApnsPushService {

    private final ApnsProperties apnsProps;

    private volatile ApnsClient apnsClient;
    private final ExecutorService executor =
        new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @PostConstruct
    public void init() {
        if (!Boolean.TRUE.equals(apnsProps.getEnabled())) {
            log.info("APNs 未启用 (notify.apns.enabled=false)，跳过初始化");
            return;
        }
        if (!hasValidConfig()) {
            log.warn("APNs 配置不完整 (keyId/teamId/bundleId/keyPath)，跳过初始化");
            return;
        }
        try {
            File keyFile = new File(apnsProps.getKeyPath());
            if (!keyFile.exists()) {
                log.warn("APNs 密钥文件不存在, path:{}", apnsProps.getKeyPath());
                return;
            }
            ApnsSigningKey signingKey = ApnsSigningKey.loadFromPkcs8File(
                keyFile, apnsProps.getTeamId(), apnsProps.getKeyId());
            String host = Boolean.TRUE.equals(apnsProps.getProduction())
                ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
            apnsClient = new ApnsClientBuilder()
                .setApnsServer(host)
                .setSigningKey(signingKey)
                .build();
            log.info("APNs 客户端初始化成功, host:{}", host);
        } catch (Exception e) {
            log.error("APNs 客户端初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (apnsClient != null) {
            try {
                apnsClient.close().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("APNs 客户端关闭异常", e);
            }
        }
        executor.shutdown();
    }

    /**
     * 异步推送
     */
    public void asyncSend(String apnsToken, String title, String body, String notifyId, Object payload) {
        if (!isAvailable()) {
            log.debug("APNs 不可用，跳过推送 title:{}, body:{}", title, body);
            return;
        }
        if (StrUtil.isBlank(apnsToken)) {
            log.debug("APNs token 为空，跳过推送 title:{}", title);
            return;
        }
        log.info("APNs 开始推送, title:{}, body:{}, tokenPrefix:{}", title, body,
            apnsToken.replaceAll("\\s+", "").substring(0, Math.min(16, apnsToken.replaceAll("\\s+", "").length())) + "...");
        executor.execute(() -> send(apnsToken, title, body, notifyId, payload));
    }

    /**
     * 同步推送
     */
    public void send(String apnsToken, String title, String body, String notifyId, Object payload) {
        if (!isAvailable() || apnsToken == null || apnsToken.isBlank()) {
            return;
        }
        String token = apnsToken.replaceAll("\\s+", "");
        try {
            SimpleApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
            payloadBuilder.setAlertTitle(title);
            payloadBuilder.setAlertBody(body);
            payloadBuilder.setSound("default");
            payloadBuilder.setBadgeNumber(1);
            if (payload != null) {
                payloadBuilder.addCustomProperty("payload", payload);
            }
            String jsonPayload = payloadBuilder.build();

            SimpleApnsPushNotification notification = (notifyId != null && !notifyId.isBlank())
                ? new SimpleApnsPushNotification(token, apnsProps.getBundleId(), jsonPayload, null, null, notifyId)
                : new SimpleApnsPushNotification(token, apnsProps.getBundleId(), jsonPayload);
            PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> future =
                apnsClient.sendNotification(notification);

            PushNotificationResponse<SimpleApnsPushNotification> response = future.get(10, TimeUnit.SECONDS);
            if (response.isAccepted()) {
                log.info("APNs 推送成功, title:{}, body:{}, tokenPrefix:{}",
                    title, body, token.substring(0, Math.min(16, token.length())) + "...");
            } else {
                log.warn("APNs 推送被拒绝, title:{}, reason:{}, tokenPrefix:{}",
                    title, response.getRejectionReason(), token.substring(0, Math.min(16, token.length())) + "...");
            }
        } catch (Exception e) {
            log.error("APNs 推送异常, title:{}, body:{}, tokenPrefix:{}",
                title, body, token.substring(0, Math.min(16, token.length())) + "...", e);
        }
    }

    public boolean isAvailable() {
        return apnsClient != null;
    }

    private boolean hasValidConfig() {
        return StrUtil.isNotBlank(apnsProps.getKeyId()) && StrUtil.isNotBlank(apnsProps.getTeamId())
            && StrUtil.isNotBlank(apnsProps.getBundleId()) && StrUtil.isNotBlank(apnsProps.getKeyPath());
    }
}
