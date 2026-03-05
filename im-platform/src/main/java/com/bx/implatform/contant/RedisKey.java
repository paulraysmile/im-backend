package com.bx.implatform.contant;

public final class RedisKey {

    /**
     * 用户状态 无值:空闲  1:正在忙
     */
    public static final String IM_USER_STATE = "chat:user:state";

    /**
     * 用户cid
     */
    public static final String IM_USER_CID = "chat:user:cid";

    /**
     * 验证码
     */
    public static final String IM_CAPTCHA = "chat:captcha";

    /**
     * 图形验证码
     */
    public static final String IM_CAPTCHA_IMAGE = "chat:captcha:img";

    /**
     * 短信验证码
     */
    public static final String IM_CAPTCHA_SMS = "chat:captcha:sms";

    /**
     * 邮箱验证码
     */
    public static final String IM_CAPTCHA_MAIL = "chat:captcha:mail";

    /**
     * 已读群聊消息位置(已读最大id)
     */
    public static final String IM_GROUP_READED_POSITION = "chat:readed:group:position";

    /**
     * 已读系统消息位置(已读最大seqNo)
     */
    public static final String IM_SYSTEM_READED_POSITION = "chat:readed:system:position";

    /**
     * 群成员最大版本号
     */
    public static final String IM_GROUP_MEMBER_MAX_VERSION = "chat:group:member:max_version";

    /**
     * 离线通知
     */
    public static final String IM_NOTIFY_OFFLINE_SESSION = "chat:notify:offline";

    /**
     * 系统消息推送序列号
     */
    public static final String IM_SM_TASK_SEQ = "chat:task:sm:seq";

    /**
     * webrtc 单人通话
     */
    public static final String IM_WEBRTC_PRIVATE_SESSION = "chat:webrtc:private:session";

    /**
     * webrtc 群通话
     */
    public static final String IM_WEBRTC_GROUP_SESSION = "chat:webrtc:group:session";

    /**
     * 用户被封禁消息队列
     */
    public static final String IM_QUEUE_USER_BANNED = "chat:queue:user:banned";

    /**
     * 群聊被封禁消息队列
     */
    public static final String IM_QUEUE_GROUP_BANNED = "chat:queue:group:banned";

    /**
     * 群聊解封消息队列
     */
    public static final String IM_QUEUE_GROUP_UNBAN = "chat:queue:group:unban";

    /**
     * 缓存是否好友：bool
     */
    public static final String IM_CACHE_FRIEND = "chat:cache:friend";

    /**
     * 缓存好友免打扰
     */
    public static final String IM_CACHE_FRIEND_DND = "chat:cache:friend_dnd";

    /**
     * 缓存群聊信息
     */
    public static final String IM_CACHE_GROUP = "chat:cache:group";

    /**
     * 缓存群聊免打扰
     */
    public static final String IM_CACHE_GROUP_DNS = "chat:cache:group_dnd";

    /**
     * 缓存是否在黑名单中：bool
     */
    public static final String IM_CACHE_BLACKLIST = "chat:cache:blacklist";

    /**
     * 缓存群聊成员id
     */
    public static final String IM_CACHE_GROUP_MEMBER_ID = "chat:cache:group_member_ids";

    /**
     * 缓存表情专辑
     */
    public static final String IM_CACHE_STICKER_ALBUMS = "chat:cache:sticker:albums";

    /**
     * 缓存表情包
     */
    public static final String IM_CACHE_STICKER_STICKERS = "chat:cache:sticker:stickers";

    /**
     * 分布式锁-群通话
     */
    public static final String IM_LOCK_RTC_GROUP = "chat:lock:rtc:group";

    /**
     * 分布式锁-系统消息推送
     */
    public static final String IM_LOCK_SM_TASK = "chat:lock:task:sm";

    /**
     * 分布式锁-清理过期文件
     */
    public static final String IM_LOCK_FILE_EXPIRE_TASK = "chat:lock:task:file:expire";

    /**
     * 分布式锁-清理无效文件
     */
    public static final String IM_LOCK_FILE_INVALID_TASK = "chat:lock:task:file:invalid";

    /**
     * 分布式锁-解除用户封禁任务
     */
    public static final String IM_LOCK_USER_UNBAN_TASK = "chat:lock:task:user:unban";

    /**
     * 分布式锁-解除群聊封禁任务
     */
    public static final String IM_LOCK_GROUP_UNBAN_TASK = "chat:lock:task:group:unban";

    /**
     * 分布式锁-添加好友
     */
    public static final String IM_LOCK_FRIEND_ADD = "chat:lock:friend:add";

    /**
     * 分布式锁-进入群聊
     */
    public static final String IM_LOCK_GROUP_ENTER = "chat:lock:group:enter";

    /**
     * 重复提交
     */
    public static final String IM_REPEAT_SUBMIT = "chat:repeat:submit";

    /**
     * 登陆二维码
     */
    public static final String IM_LOGIN_QRCODE = "chat:login:qrcode";

    /**
     * 群名片token
     */
    public static final String IM_GROUP_CARD_TOKEN = "chat:group:card:token";

    /**
     * 群聊加入token
     */
    public static final String IM_GROUP_QRCODE_TOKEN = "chat:group:qrcode:token";

    /**
     * app是否正在审核中
     */
    public static final String IM_APP_REVIEW = "chat:app:review";

    /**
     * 用户每日添加好友计数
     */
    public static final String IM_FRIEND_APPLY_COUNT = "chat:friend:apply:count";

}
