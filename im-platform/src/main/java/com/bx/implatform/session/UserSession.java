package com.bx.implatform.session;

import com.bx.imcommon.model.IMSessionInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserSession extends IMSessionInfo {

    /** 用户名称 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    /** 归属企业 id，用于动态表名路由（私聊/群聊/群成员/群消息分表） */
    private Long companyId;
}
