package com.zyq.chirp.adviceserver.util;

import jakarta.annotation.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUtil {
    @Resource
    RedisTemplate redisTemplate;

    public boolean isSubscribe(String topic) {
        Object pubsub = redisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.execute("PUBSUB", "NUMSUB".getBytes(), topic.getBytes());
            }
        });
        System.out.println(pubsub);
        return true;
    }
}
