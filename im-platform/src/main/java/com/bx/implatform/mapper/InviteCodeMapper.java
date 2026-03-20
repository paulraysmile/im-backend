package com.bx.implatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bx.implatform.entity.InviteCode;

/**
 * 邀请码 Mapper，不参与动态表名，始终查主库 im_invite_code。
 */
public interface InviteCodeMapper extends BaseMapper<InviteCode> {
}
