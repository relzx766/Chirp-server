package com.zyq.chirp.adviceserver.service.impl;

import com.zyq.chirp.adviceserver.mq.listener.RedisSubscribeListener;
import com.zyq.chirp.adviceserver.service.WsService;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class WsServiceImpl implements WsService {
    private static final Map<String, RedisSubscribeListener> redisPSubMap = new HashMap<>();
    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    RedisMessageListenerContainer redisMessageListenerContainer;
    @Value("${mq.topic.socket-connect}")
    private String connectTopic;
    @Value("${mq.topic.socket-disconnect}")

    private String disconnectTopic;
    @Value("${mq.topic.site-message.user}")
    private String messageTopic;

    @Override
    public void connect(Long userId, Session session) {
        kafkaTemplate.send(connectTopic, userId);
        if (redisPSubMap.containsKey(userId.toString())) {
            redisPSubMap.get(userId.toString()).addSession(session);
        } else {
            RedisSubscribeListener subscribeListener = new RedisSubscribeListener(session);
            redisPSubMap.put(userId.toString(), subscribeListener);
            redisMessageListenerContainer.addMessageListener(subscribeListener, new ChannelTopic(messageTopic + userId));
        }
    }

    @Override
    public void disconnect(Long userId, Session session) {
        kafkaTemplate.send(disconnectTopic, userId);
        RedisSubscribeListener listener = redisPSubMap.get(userId.toString());
        listener.removeSession(session.getId());
        if (listener.getOpenSessionSize() <= 0) {
            redisMessageListenerContainer.removeMessageListener(listener);
            redisPSubMap.remove(userId.toString());
        }
    }
}
