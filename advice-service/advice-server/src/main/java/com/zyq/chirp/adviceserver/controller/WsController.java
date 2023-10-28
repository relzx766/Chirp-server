package com.zyq.chirp.adviceserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.enums.MessageTypeEnum;
import com.zyq.chirp.adviceserver.domain.enums.CacheKey;
import com.zyq.chirp.adviceserver.domain.enums.ChatStatusEnum;
import com.zyq.chirp.adviceserver.domain.enums.WsActionEnum;
import com.zyq.chirp.adviceserver.mq.dispatcher.MessageDispatcher;
import com.zyq.chirp.adviceserver.mq.listener.RedisSubscribeListener;
import com.zyq.chirp.adviceserver.service.ChatService;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/interaction/{userId}")
@Component
@Slf4j
public class WsController {

    static KafkaTemplate<String, Object> kafkaTemplate;
    static String connectTopic;
    static String disconnectTopic;
    static String messageTopic;
    static MessageDispatcher messageDispatcher;
    static RedisTemplate<String, Object> redisTemplate;
    static ObjectMapper objectMapper;
    static ChatService chatService;
    static RedisMessageListenerContainer redisMessageListenerContainer;
    private static ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();
    private static CopyOnWriteArraySet<WsController> webSocketSet = new CopyOnWriteArraySet<>();
    String socketId;
    RedisSubscribeListener subscribeListener;

    @Autowired
    public void setRedisMessageListenerContainer(RedisMessageListenerContainer redisMessageListenerContainer) {
        WsController.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        WsController.objectMapper = objectMapper;
    }

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        WsController.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setPrivateMessageService(ChatService chatService) {
        WsController.chatService = chatService;
    }

    @Value("${mq.topic.socket-connect}")

    public void setConnectTopic(String connectTopic) {
        WsController.connectTopic = connectTopic;
    }

    @Value("${mq.topic.socket-disconnect}")

    public void setDisconnectTopic(String disconnectTopic) {
        WsController.disconnectTopic = disconnectTopic;
    }

    @Value("${mq.topic.site-message.user}")

    public void setMessageTopic(String messageTopic) {
        WsController.messageTopic = messageTopic;
    }

    @Autowired
    public void setNoticeDispatcher(MessageDispatcher messageDispatcher) {
        WsController.messageDispatcher = messageDispatcher;
    }

    @Autowired
    public void setKafkaTemplate(KafkaTemplate<String, Object> kafkaTemplate) {
        WsController.kafkaTemplate = kafkaTemplate;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        log.info("WebSocket建立连接中,连接用户ID：{}", userId);
        socketId = session.getId();
        webSocketSet.add(this);
        sessionPool.put(userId, session);
        redisTemplate.opsForHash().put(CacheKey.BOUND_CONNECT_INFO.getKey(), STR. "\{ userId }:\{ socketId }" , socketId);
        kafkaTemplate.send(connectTopic, userId);
        subscribeListener = new RedisSubscribeListener(session);
        redisMessageListenerContainer.addMessageListener(subscribeListener, new ChannelTopic(messageTopic + userId));
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") Long userId) {

        if (WsActionEnum.HEARTBEAT.name().equalsIgnoreCase(message)) {
            session.getAsyncRemote().sendText(WsActionEnum.HEARTBEAT.name());
        } else {
            try {
                ChatDto chatDto = objectMapper.readValue(message, ChatDto.class);
                chatDto.setSenderId(userId);
                chatService.send(chatDto);
                chatDto.setStatus(ChatStatusEnum.UNREAD.getStatus());
                session.getAsyncRemote().sendText(objectMapper.writeValueAsString(Map.of(MessageTypeEnum.CHAT.name(), List.of(chatDto))));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new ChirpException(Code.ERR_BUSINESS, "json转化失败,请检查消息格式");
            }

        }
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

    @OnError
    public void onError(Session session, @PathParam("userId") Long userId, Throwable error) {
        log.error("{}", error);
    }

}
