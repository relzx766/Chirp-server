package com.zyq.chirp.common.mq;

import com.zyq.chirp.common.model.DelayMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DefaultKafkaProducer<T> {

    KafkaTemplate<String, Object> kafkaTemplate;
    RedisTemplate redisTemplate;

    public DefaultKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, RedisTemplate redisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
    }

    public void send(String topic, T t) {
        kafkaTemplate.send(topic, t);
    }

    public void delaySend(String topic, T t, Duration delay) {
        DelayMessage message = new DelayMessage(t, delay);
        kafkaTemplate.send(topic, message);
    }

    /**
     * 避免重复发送,使用string存储，hk:k:v,v为固定的int值1
     *
     * @param k      k
     * @param topic  hk
     * @param t
     * @param expire 保护时间
     */
    public void avoidRedundancySend(String k, String topic, T t, Duration expire) {
        ValueOperations<String, Integer> operations = redisTemplate.opsForValue();
        Boolean notExists = operations.setIfAbsent(topic + ":" + k, 1, expire);
        if (notExists) {
            send(topic, t);
        }
    }
}
