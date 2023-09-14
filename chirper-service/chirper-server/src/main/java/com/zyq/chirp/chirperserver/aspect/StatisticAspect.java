package com.zyq.chirp.chirperserver.aspect;

import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.common.util.SpElUtil;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class StatisticAspect {
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Pointcut("@annotation(com.zyq.chirp.chirperserver.aspect.Statistic)")
    public void pointcut() {
    }

    @AfterReturning("pointcut()")
    public void after(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Statistic annotation = method.getAnnotation(Statistic.class);
        for (CacheKey cacheKey : annotation.key()) {
            BoundHashOperations<String, String, Integer> operations = redisTemplate.boundHashOps(cacheKey.getKey());
            String id = SpElUtil.generateKeyBySPEL(annotation.id(), joinPoint);
            operations.increment(id, annotation.delta());
        }
    }

}
