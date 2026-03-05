package com.bx.implatform.session;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 离线通知session
 * @author: Blue
 * @date: 2024-08-21
 * @version: 1.0
 */
@Data
public class OfflineNotifySession {
    /**
     *  好友id集合
     */
    private Set<Long> friendIds = new HashSet<>();

    /**
     *  群聊id集合
     */
    private Set<Long> groupIds  = new HashSet<>() ;

    /**
     *  消息数量
     */
    private Integer messageSize = 0;

}
