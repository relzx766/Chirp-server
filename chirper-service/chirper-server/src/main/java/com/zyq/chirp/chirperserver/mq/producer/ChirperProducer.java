package com.zyq.chirp.chirperserver.mq.producer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class ChirperProducer<T> {

    @Resource
    KafkaTemplate<String, T> kafkaTemplate;
    @Resource
    RedisTemplate<String, Integer> redisTemplate;

    public void send(String topic, T t) {
        kafkaTemplate.send(topic, t);
    }

    /**
     * 短期不重复,键为topic:k
     *
     * @param k
     * @param topic
     * @param t
     * @param expire 过期时间
     */

    public void avoidSend(String k, String topic, T t, Duration expire) {
        ValueOperations<String, Integer> operations = redisTemplate.opsForValue();
        Boolean notExists = operations.setIfAbsent(topic + ":" + k, 1, expire);
        if (notExists) {
            kafkaTemplate.send(topic, t);
        }
    }
}
