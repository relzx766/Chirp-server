/*
package com.zyq.chirp.chirperserver.task;

import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import com.zyq.chirp.chirperserver.service.ChirperService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@Slf4j
public class ChirperTask {
    @Resource
    RedisTemplate redisTemplate;
    @Resource
    ChirperService chirperService;
    @Value("${default-config.per-save-like-size}")
    Long saveLimit;

    @Async
    @Scheduled(fixedDelay = 4000)
    public void saveViewTask() {
        log.info("持久化浏览量 start----");
        BoundHashOperations<String, String, Integer> operations = redisTemplate.boundHashOps(CacheKey.VIEW_COUNT_BOUND_KEY.getKey());
        Objects.requireNonNull(operations.keys())
                .stream()
                .limit(saveLimit)
                .forEach(key -> {
                    Integer count = operations.get(key);
                    int flag = chirperService.updateView(Long.valueOf(key), count);
                    if (flag > 0) {
                        operations.delete(key);
                    }
                });
        log.info("----end 持久化浏览量");
    }

    @Async
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void saveForwardCountTask() {
        log.info("持久化转发量 start----");
        BoundHashOperations<String, String, Integer> operations = redisTemplate.boundHashOps(CacheKey.FORWARD_COUNT_BOUND_KEY.getKey());
        Objects.requireNonNull(operations.keys())
                .stream()
                .limit(saveLimit)
                .forEach(key -> {
                    Integer count = operations.get(key);
                    int flag = chirperService.updateForward(Long.valueOf(key), count);
                    if (flag > 0) {
                        operations.delete(key);
                    }
                });
        log.info("----end 持久化转发量");
    }

}
*/
