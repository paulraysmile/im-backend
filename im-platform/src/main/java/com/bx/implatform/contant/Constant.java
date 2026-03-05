package com.bx.implatform.contant;

public final class Constant {

    /**
     * 系统用户id
     */
    public static final Long SYS_USER_ID = 0L;

    /**
     * 最大图片上传大小
     */
    public static final Long MAX_IMAGE_SIZE = 20 * 1024 * 1024L;

    /**
     * 最大上传文件大小
     */
    public static final Long MAX_FILE_SIZE = 20 * 1024 * 1024L;

    /**
     * 最大上传视频大小
     */
    public static final Long MAX_VIDEO_SIZE = 50 * 1024 * 1024L;

    /**
     * 最大文件名长度
     */
    public static final Long MAX_FILE_NAME_LENGTH = 128L;
    /**
     * 大群人数上限
     */
    public static final Long MAX_LARGE_GROUP_MEMBER = 3000L;

    /**
     * 普通群人数上限
     */
    public static final Long MAX_NORMAL_GROUP_MEMBER = 500L;

    /**
     * 好友申请列表上线
     */
    public static final Long MAX_PRIEND_APPLY = 20L;

    /**
     * 扫码登录过期时间 (单位:分钟)
     */
    public static final Integer QR_LOGIN_EXPIRE_MINUTES = 2;

    /**
     * 文字消息最大长度
     */
    public static final Long MAX_MESSAGE_LENGTH = 1024L;

    /**
     * 用户自定义表情包数量上限
     */
    public static final Long MAX_CUSTOM_STICKET_SIZE = 50L;

    /**
     * 名片分享过期时间
     */
    public static final Long SHARE_CARD_EXPIRED_DAYS = 7L;

    /**
     * 群二维码过期时间
     */
    public static final Long GROUP_QRCODE_EXPIRED_DAYS = 30L;

    /**
     * 每日添加好友最大数量限制
     */
    public static final Long DAILY_FRIEND_APPLY_LIMIT = 50L;

    /**
     * 离线消息最大拉取时间(天)
     */
    public static final Long MAX_OFFLINE_MESSAGE_DAYS = 30L;

    /**
     * 系统消息最大拉取时间(天)
     */
    public static final Long MAX_SYSTEM_MESSAGE_DAYS = 90L;
}
