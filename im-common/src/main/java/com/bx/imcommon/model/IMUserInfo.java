package com.bx.imcommon.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Blue
 * @date: 2023-09-24 09:23:11
 * @version: 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IMUserInfo {

    /** 兼容仅 (id, terminal) 的构造，deviceId 为 null */
    public IMUserInfo(Long id, Integer terminal) {
        this.id = id;
        this.terminal = terminal;
        this.deviceId = null;
    }

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户终端类型 IMTerminalType
     */
    private Integer terminal;

    /**
     * 设备 id（多设备时区分同一 terminal 下不同连接，如 WEB 多台 PC）
     * 为空时表示单设备终端（如 APP 仅允许一台）
     */
    private String deviceId;

}
