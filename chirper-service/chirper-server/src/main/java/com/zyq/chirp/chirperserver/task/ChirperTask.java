
package com.zyq.chirp.chirperserver.task;

import com.zyq.chirp.chirperserver.service.ChirperService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChirperTask {
    @Resource
    RedisTemplate redisTemplate;
    @Resource
    ChirperService chirperService;
    @Value("${default-config.per-save-like-size}")
    Long saveLimit;

    /*    @Async
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
        }*/
    @Async
    @Scheduled(cron = "0 */5 * * * *")
    public void activeDelayTask() {
        log.info("激活延时推文 start----");
        chirperService.activeDelayAuto();
        log.info("激活延时推文 end----");
    }
}

