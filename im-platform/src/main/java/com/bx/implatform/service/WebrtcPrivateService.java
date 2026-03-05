package com.bx.implatform.service;

import com.bx.implatform.vo.WebrtcPrivateInfoVO;

/**
 * webrtc 通信服务
 *
 * @author
 */
public interface WebrtcPrivateService {

    void setup(Long uid, String mode);

    void accept(Long uid);

    void reject(Long uid);

    void cancel(Long uid);

    void failed(Long uid, String reason);

    void handup(Long uid);

    void offer(Long uid,String offer);

    void answer(Long uid,String answer);

    void forceQuit(Long uid);

    void candidate(Long uid, String candidate);

    void heartbeat(Long uid);

    WebrtcPrivateInfoVO info(Long uid);

}
