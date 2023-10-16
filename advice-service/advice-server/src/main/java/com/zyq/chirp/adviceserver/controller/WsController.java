package com.zyq.chirp.adviceserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceserver.domain.enums.CacheKey;
import com.zyq.chirp.adviceserver.mq.dispatcher.NoticeDispatcher;
import com.zyq.chirp.adviceserver.service.NoticeMessageService;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
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
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/interaction/{userId}")
@Component
@Slf4j
public class WsController {

    static final String PAGE = "/page";
    static NoticeMessageService noticeMessageService;
    static KafkaTemplate<String, Object> kafkaTemplate;
    static String connectTopic;
    static String disconnectTopic;
    static NoticeDispatcher noticeDispatcher;
    static RedisTemplate<String, Object> redisTemplate;
    static ObjectMapper objectMapper;
    private static ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();
    private static CopyOnWriteArraySet<WsController> webSocketSet = new CopyOnWriteArraySet<>();
    String socketId;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        WsController.objectMapper = objectMapper;
    }

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        WsController.redisTemplate = redisTemplate;
    }

    @Value("${mq.topic.socket-connect}")

    public void setConnectTopic(String connectTopic) {
        WsController.connectTopic = connectTopic;
    }

    @Value("${mq.topic.socket-disconnect}")

    public void setDisconnectTopic(String disconnectTopic) {
        WsController.disconnectTopic = disconnectTopic;
    }

    @Autowired
    public void setNoticeDispatcher(NoticeDispatcher noticeDispatcher) {
        WsController.noticeDispatcher = noticeDispatcher;
    }

    @Autowired
    public void setKafkaTemplate(KafkaTemplate<String, Object> kafkaTemplate) {
        WsController.kafkaTemplate = kafkaTemplate;
    }

    @Autowired

    public void setInteractionMessageService(NoticeMessageService noticeMessageService) {
        WsController.noticeMessageService = noticeMessageService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        log.info("WebSocket建立连接中,连接用户ID：{}", userId);
        socketId = session.getId();
        webSocketSet.add(this);
        sessionPool.put(userId, session);
        redisTemplate.opsForHash().put(CacheKey.BOUND_CONNECT_INFO.getKey(), STR. "\{ userId }:\{ socketId }" , socketId);
        kafkaTemplate.send(connectTopic, userId);
        noticeDispatcher.addSession(userId, session);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") Long userId) {

    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        log.info("WebSocket断开连接,用户ID：{}", userId);
        webSocketSet.remove(this);
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(CacheKey.BOUND_CONNECT_INFO.getKey());
        operations.delete(STR. "\{ userId }:\{ socketId }" );
        try (Cursor<Map.Entry<Object, Object>> cursor = operations.scan(ScanOptions.scanOptions().match(STR. "\{ userId }*" ).build())) {
            if (!cursor.hasNext()) {
                kafkaTemplate.send(disconnectTopic, userId);
            }
        }
    }
}
