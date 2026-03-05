package com.bx.implatform.session;

import com.bx.imcommon.model.IMUserInfo;
import lombok.Data;

/*
 * webrtc 会话信息
 * @Author Blue
 * @Date 2022/10/21
 */
@Data
public class WebrtcPrivateSession {

    /**
     * 会话唯一id
     */
    private Long chatId;
    /**
     *  通话发起者
     */
    private IMUserInfo host;

    /**
     *  通话被邀请者
     */
    private IMUserInfo acceptor;

    /**
     *  通话模式
     */
    private String mode;

    /**
     * 呼叫时间戳
     */
    private Long  setupTimeStamp;

    /**
     * 开始聊天时间戳
     */
    private Long  chatTimeStamp;
}
