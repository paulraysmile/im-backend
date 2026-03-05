package com.bx.implatform.thirdparty;

import com.alibaba.fastjson.JSON;
import com.bx.implatform.config.props.NotifyProperties;
import com.getui.push.v2.sdk.api.PushApi;
import com.getui.push.v2.sdk.common.ApiResult;
import com.getui.push.v2.sdk.dto.req.Audience;
import com.getui.push.v2.sdk.dto.req.Settings;
import com.getui.push.v2.sdk.dto.req.message.PushChannel;
import com.getui.push.v2.sdk.dto.req.message.PushDTO;
import com.getui.push.v2.sdk.dto.req.message.PushMessage;
import com.getui.push.v2.sdk.dto.req.message.android.AndroidDTO;
import com.getui.push.v2.sdk.dto.req.message.android.GTNotification;
import com.getui.push.v2.sdk.dto.req.message.android.ThirdNotification;
import com.getui.push.v2.sdk.dto.req.message.android.Ups;
import com.getui.push.v2.sdk.dto.req.message.ios.Alert;
import com.getui.push.v2.sdk.dto.req.message.ios.Aps;
import com.getui.push.v2.sdk.dto.req.message.ios.IosDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author Blue
 * @version 1.0
 * @date 2024-07-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UniPushService {

    @Lazy
    @Autowired
    private PushApi pushApi;

    private final NotifyProperties notifyProps;

    /**
     * 使用一个独立的线程池去进行离线通知推送，避免对主业务有干扰
     * 线程数量: cpu核心数量的一半
     * 任务数量上限: 50000
     * 拒绝策略: 直接丢弃，不推送(消息提醒如果过于延迟，推过去了也没有意义)
     */
    private static final int CPU_CORE_SIZE = Runtime.getRuntime().availableProcessors();
    private final ExecutorService excutor =
        new ThreadPoolExecutor(CPU_CORE_SIZE / 2, CPU_CORE_SIZE / 2, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(50000), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardPolicy());

    /**
     * 异步推送消息
     *
     * @param cid      客户id
     * @param title    通知栏标题
     * @param body     通知栏内容
     * @param logo     通知栏logo,默认是app的logo
     * @param notifyId 通知id,相同notifyId的消息会被覆盖
     * @param payload  自定义内容，一般用于页面跳转
     */
    public void asyncSend(String cid, String title, String body, String logo, String notifyId, Object payload) {
        excutor.execute(() -> send(cid, title, body, logo, notifyId, payload));
    }

    /**
     * 推送消息
     *
     * @param cid      客户id
     * @param title    通知栏标题
     * @param body     通知栏内容
     * @param logo     通知栏logo,默认是app的logo
     * @param notifyId 通知id,相同notifyId的消息会被覆盖
     * @param payload  自定义内容，一般用于页面跳转
     */
    public void send(String cid, String title, String body, String logo, String notifyId, Object payload) {
        PushDTO<Audience> pushDTO = new PushDTO<>();
        // 设置推送参数，requestid需要每次变化唯一
        pushDTO.setRequestId(System.currentTimeMillis() + "");
        Settings settings = new Settings();
        // 消息有效期，走厂商消息必须设置该值
        settings.setTtl(3600000);
        pushDTO.setSettings(settings);
        // 设置接收人信息
        Audience audience = new Audience();
        audience.addCid(cid);
        pushDTO.setAudience(audience);
        // 个推通道
        PushMessage pushMessage = new PushMessage();
        pushDTO.setPushMessage(pushMessage);
        GTNotification gtNotification = new GTNotification();
        gtNotification.setTitle(title);
        gtNotification.setBody(body);
        gtNotification.setClickType("startapp");
        gtNotification.setNotifyId(notifyId);
        gtNotification.setLogoUrl(logo);
        gtNotification.setBadgeAddNum("1");
        pushMessage.setNotification(gtNotification);
        // 设置离线推送时的消息体
        PushChannel pushChannel = new PushChannel();
        // 安卓离线厂商通道推送的消息体
        AndroidDTO androidDTO = new AndroidDTO();
        Ups ups = new Ups();
        ThirdNotification thirdNotification = new ThirdNotification();
        ups.setNotification(thirdNotification);
        ups.setOptions(buildOptions(logo));
        thirdNotification.setTitle(title);
        thirdNotification.setBody(body);
        thirdNotification.setNotifyId(notifyId);
        if (!Objects.isNull(payload)) {
            // 打开自定义页面
            thirdNotification.setClickType("intent");
            thirdNotification.setIntent(buildIntent(payload));
        } else {
            // 打开首页
            thirdNotification.setClickType("startapp");
        }
        androidDTO.setUps(ups);
        pushChannel.setAndroid(androidDTO);
        // ios离线apn通道推送的消息体
        Alert alert = new Alert();
        alert.setTitle(title);
        alert.setBody(body);
        Aps aps = new Aps();
        // 0：普通通知消息  1:静默推送(无通知栏消息)，静默推送时不需要填写其他参数。苹果建议1小时最多推送3条静默消息
        aps.setContentAvailable(0);
        // default: 系统铃声  不填:无声
        aps.setSound("default");
        aps.setAlert(alert);

        IosDTO iosDTO = new IosDTO();
        iosDTO.setAps(aps);
        iosDTO.setType("notify");
        iosDTO.setAutoBadge("+1");
        if (!Objects.isNull(payload)) {
            iosDTO.setPayload(JSON.toJSONString(payload));
        }
        iosDTO.setApnsCollapseId(notifyId);
        pushChannel.setIos(iosDTO);
        pushDTO.setPushChannel(pushChannel);
        // 推送
        ApiResult<Map<String, Map<String, String>>> apiResult = pushApi.pushToSingleByCid(pushDTO);
        if (apiResult.isSuccess()) {
            log.info("推送成功,{}", apiResult.getData());
        } else {
            log.info("推送失败,code:{},msg:{}", apiResult.getCode(), apiResult.getMsg());
        }
    }

    private Map<String, Map<String, Object>> buildOptions(String logo) {
        Map<String, Map<String, Object>> options = new HashMap<>();
        // 小米
        Map<String, Object> xm = new HashMap<>();
        xm.put("/extra.notification_style_type", 1);
        xm.put("/extra.notification_large_icon_uri", logo);
        xm.put("/extra.channel_id", notifyProps.getManufacturer().getXmChannelId());
        options.put("XM", xm);
        // 华为
        Map<String, Object> hw = new HashMap<>();
        hw.put("/message/android/notification/badge/add_num", 1);
        hw.put("/message/android/notification/badge/class", "io.dcloud.PandoraEntry");
        hw.put("/message/android/notification/image", logo);
        hw.put("/message/android/category", notifyProps.getManufacturer().getHwCategory());
        hw.put("/message/android/notification/importance", "NORMAL");
        options.put("HW", hw);
        // 荣耀
        Map<String, Object> ho = new HashMap<>();
        ho.put("/android/notification/badge/addNum", 1);
        ho.put("/android/notification/badge/badgeClass", "io.dcloud.PandoraEntry");
        ho.put("/android/notification/image", logo);
        ho.put("/android/notification/importance", "NORMAL");
        options.put("HO", ho);
        // vivo
        Map<String, Object> vv = new HashMap<>();
        vv.put("/category", notifyProps.getManufacturer().getVvCategory());
        options.put("VV", vv);
        // oppo
        Map<String, Object> op = new HashMap<>();
        op.put("/category", notifyProps.getManufacturer().getOpCategory());
        op.put("/notify_level", 2);
        options.put("OP", op);
        // 魅族
        Map<String, Object> mz = new HashMap<>();
        op.put("/noticeMsgType", 1);
        options.put("MZ", mz);
        // 调试模式
        if (notifyProps.getDebug()) {
            hw.put("/message/android/target_user_type", 1);
            ho.put("/android/targetUserType", 1);
            vv.put("/pushMode", 1);
        }
        return options;
    }

    private String buildIntent(Object payload) {
        String url = "intent://io.dcloud.unipush/?#Intent;scheme=unipush;launchFlags=0x4000000;S.UP-OL-SU=true;";
        url += "component=" + notifyProps.getPackageName() + "/io.dcloud.PandoraEntry;";
        url += "S.payload=" + JSON.toJSONString(payload) + ";";
        url += "end";
        return url;
    }
}
