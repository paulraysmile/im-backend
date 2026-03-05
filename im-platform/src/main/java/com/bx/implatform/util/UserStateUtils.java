package com.bx.implatform.util;

import cn.hutool.core.util.StrUtil;
import com.bx.implatform.contant.RedisKey;
import com.bx.implatform.enums.UserStateType;
import com.bx.implatform.vo.UserStateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author: Blue
 * @date: 2024-06-10
 * @version: 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStateUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public void setFree(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_STATE,userId);
        redisTemplate.delete(key);
    }

    public void  inRrivateRtc(Long userId, Long friendId){
        UserStateVO state = new UserStateVO();
        state.setType(UserStateType.RTC_PRIVATE.getCode());
        state.setData(friendId);
        String key = StrUtil.join(":", RedisKey.IM_USER_STATE,userId);
        redisTemplate.opsForValue().set(key,state,30, TimeUnit.SECONDS);
    }

    public void  inGroupRtc(Long userId, Long groupId){
        UserStateVO state = new UserStateVO();
        state.setType(UserStateType.RTC_GROUP.getCode());
        state.setData(groupId);
        String key = StrUtil.join(":", RedisKey.IM_USER_STATE,userId);
        redisTemplate.opsForValue().set(key,state,30, TimeUnit.SECONDS);
    }

    public void expire(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_STATE,userId);
        redisTemplate.expire(key,30, TimeUnit.SECONDS);
    }

    public Boolean isBusy(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_STATE,userId);
        return  redisTemplate.hasKey(key);
    }

    public UserStateVO getState(Long userId){
        String key = StrUtil.join(":", RedisKey.IM_USER_STATE,userId);
        return (UserStateVO)redisTemplate.opsForValue().get(key);
    }
}
