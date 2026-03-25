package com.bx.implatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bx.implatform.entity.GroupMember;
import com.bx.implatform.vo.GroupInfoVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    Integer isNotAllowAdd(@Param("sendId") Long sendId, @Param("recvId") Long recvId);

    List<GroupInfoVO> findSameGroups(@Param("userId") Long userId, @Param("friendId") Long friendId);

}
