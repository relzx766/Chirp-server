package com.zyq.chirp.adviceserver.controller;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.domain.enums.CacheKey;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
import com.zyq.chirp.adviceserver.service.strategy.context.MessageContext;
import com.zyq.chirp.adviceserver.service.strategy.impl.InteractionMessageStrategy;
import jakarta.annotation.Resource;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/interaction/{userId}")
@Component
@Slf4j
public class InteractionController {
    static InteractionMessageStrategy messageStrategy;
    static InteractionMessageService interactionMessageService;
    static Map<String, List<KafkaMessageListenerContainer>> containerMap;
    static KafkaTemplate<String, Object> kafkaTemplate;
    static String connectTopic;
    static String disconnectTopic;
    static RedisTemplate<String, Object> redisTemplate;
    private static ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();
    private static CopyOnWriteArraySet<InteractionController> webSocketSet = new CopyOnWriteArraySet<>();
    private String socketId;

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        InteractionController.redisTemplate = redisTemplate;
    }

    @Value("${mq.topic.socket-connect}")

    public void setConnectTopic(String connectTopic) {
        InteractionController.connectTopic = connectTopic;
    }

    @Value("${mq.topic.socket-disconnect}")

    public void setDisconnectTopic(String disconnectTopic) {
        InteractionController.disconnectTopic = disconnectTopic;
    }

    @Autowired
    public void setMessageStrategy(InteractionMessageStrategy messageStrategy) {
        InteractionController.messageStrategy = messageStrategy;
    }

    @Autowired
    public void setKafkaTemplate(KafkaTemplate<String, Object> kafkaTemplate) {
        InteractionController.kafkaTemplate = kafkaTemplate;
    }

    @Autowired

    public void setInteractionMessageService(InteractionMessageService interactionMessageService) {
        InteractionController.interactionMessageService = interactionMessageService;
    }

    @Resource
    public void setContainerMap(Map<String, List<KafkaMessageListenerContainer>> containerMap) {
        InteractionController.containerMap = containerMap;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        log.info("WebSocket建立连接中,连接用户ID：{}", userId);
        this.socketId = session.getId();
        webSocketSet.add(this);
        sessionPool.put(userId, session);
        List<SiteMessageDto> messages = interactionMessageService.getByReceiverId(userId);
        redisTemplate.opsForHash().put(CacheKey.BOUND_CONNECT_INFO.getKey(), STR. "\{ userId }:\{ this.socketId }" , this.socketId);
        kafkaTemplate.send(connectTopic, userId);
        new MessageContext(messageStrategy).send(messages, session, userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        log.info("WebSocket断开连接,用户ID：{}", userId);
        webSocketSet.remove(this);
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(CacheKey.BOUND_CONNECT_INFO.getKey());
        operations.delete(STR. "\{ userId }:\{ this.socketId }" );
        try (Cursor<Map.Entry<Object, Object>> cursor = operations.scan(ScanOptions.scanOptions().match(STR. "\{ userId }*" ).build())) {
            if (!cursor.hasNext()) {
                kafkaTemplate.send(disconnectTopic, userId);
            }
        }
        List<KafkaMessageListenerContainer> container = containerMap.get(userId.toString());
        container.forEach(con -> {
            if (con.isRunning()) {
                log.info("销毁kafka容器{}", con.getListenerId());
                con.stop();
                con.destroy();
            }
        });
    }
}
