package com.bx.implatform.aspect;

import com.bx.implatform.config.props.NotifyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author: blue
 * @date: 2024-08-25
 * @version: 1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NotifyCheckAspect {

    private final NotifyProperties notifyProperties;

    @Around("@annotation(com.bx.implatform.annotation.NotifyCheck)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if(!notifyProperties.getEnable()){
            return null;
        }
        return joinPoint.proceed();
    }

}
