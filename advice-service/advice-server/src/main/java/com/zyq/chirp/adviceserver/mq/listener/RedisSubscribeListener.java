package com.zyq.chirp.adviceserver.mq.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.config.SpringbootContext;
import com.zyq.chirp.adviceserver.domain.enums.MessageTypeEnums;
import com.zyq.chirp.adviceserver.strategy.impl.ChatPushStrategy;
import com.zyq.chirp.adviceserver.strategy.impl.NoticePushStrategy;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class RedisSubscribeListener implements MessageListener {
    private final Map<String, Session> sessionMap = new HashMap<>();
    NoticePushStrategy noticePushStrategy;
    RedisTemplate redisTemplate;
    ObjectMapper objectMapper;
    ChatPushStrategy chatPushStrategy;

    public RedisSubscribeListener(Session session) {
        ApplicationContext context = SpringbootContext.getContext();
        noticePushStrategy = context.getBean(NoticePushStrategy.class);
        chatPushStrategy = context.getBean(ChatPushStrategy.class);
        objectMapper = context.getBean(ObjectMapper.class);
        redisTemplate = (RedisTemplate) context.getBean("redisTemplate");
        this.sessionMap.put(session.getId(), session);

    }

    public RedisSubscribeListener() {
        ApplicationContext context = SpringbootContext.getContext();
        noticePushStrategy = context.getBean(NoticePushStrategy.class);
        redisTemplate = (RedisTemplate) context.getBean("redisTemplate");
    }

    public int getOpenSessionSize() {
        return sessionMap.values().stream().filter(Session::isOpen).toList().size();
    }

    public void addSession(Session session) {
        sessionMap.put(session.getId(), session);
    }

    public void removeSession(String id) {
        sessionMap.remove(id);
    }
    @Override
    public void onMessage(@NotNull Message message, byte[] pattern) {
        sessionMap.forEach((sessionId, session) -> {
            if (session != null && session.isOpen()) {
                try {
                    Map<String, String> map = objectMapper.readValue(message.getBody(), new TypeReference<>() {
                    });
                    List<NotificationDto> notice = new ArrayList<>();
                    List<ChatDto> chatDtos = new ArrayList<>();
                    map.forEach((type, siteMessageStr) -> {
                        try {
                            if (MessageTypeEnums.CHAT.name().equals(type)) {
                                List<ChatDto> chatDtoList = objectMapper.readValue(siteMessageStr, new TypeReference<>() {
                                });
                                chatDtos.addAll(chatDtoList);
                            }
                            if (MessageTypeEnums.NOTICE.name().equals(type)) {
                                List<NotificationDto> notificationDtos = objectMapper.readValue(siteMessageStr, new TypeReference<>() {
                                });
                                notice.addAll(notificationDtos);
                            }
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                    });
                    if (!notice.isEmpty()) {
                        noticePushStrategy.send(notice, List.of(session));
                    }
                    if (!chatDtos.isEmpty()) {
                        chatPushStrategy.send(chatDtos, List.of(session));
                    }
                } catch (Exception e) {
                    log.info("", e);
                }
            }
        });

    }
}
