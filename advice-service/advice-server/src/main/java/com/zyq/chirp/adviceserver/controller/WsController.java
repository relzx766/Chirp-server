package com.zyq.chirp.adviceserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.domain.enums.MessageTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.WsActionEnum;
import com.zyq.chirp.adviceserver.mq.dispatcher.MessageDispatcher;
import com.zyq.chirp.adviceserver.service.ChatService;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
    static MessageDispatcher messageDispatcher;
    static RedisTemplate<String, Object> redisTemplate;
    static ObjectMapper objectMapper;
    static ChatService chatService;

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

    @Autowired
    public void setPrivateMessageService(ChatService chatService) {
        WsController.chatService = chatService;
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
        chatService.connect(userId, session);
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
                session.getAsyncRemote().sendText(objectMapper.writeValueAsString(Map.of(MessageTypeEnums.CHAT.name(), List.of(chatDto))));
            } catch (JsonProcessingException e) {
                log.error("", e);
                throw new ChirpException(Code.ERR_BUSINESS, "json转化失败,请检查消息格式");
            }

        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        log.info("WebSocket断开连接,用户ID：{}", userId);
        webSocketSet.remove(this);


    }

    @OnError
    public void onError(Session session, @PathParam("userId") Long userId, Throwable error) {
        log.error("", error);
    }

}
