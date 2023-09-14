package com.zyq.chirp.chirperserver.mq.producer;

import com.zyq.chirp.common.mq.DefaultKafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChirperProducer<T> extends DefaultKafkaProducer<T> {

    @Autowired
    public ChirperProducer(KafkaTemplate<String, Object> kafkaTemplate, RedisTemplate redisTemplate) {
        super(kafkaTemplate, redisTemplate);
    }
}
