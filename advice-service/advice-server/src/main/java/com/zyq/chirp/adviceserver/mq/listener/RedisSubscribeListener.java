package com.zyq.chirp.adviceserver.mq.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceclient.enums.MessageTypeEnum;
import com.zyq.chirp.adviceserver.config.SpringbootContext;
import com.zyq.chirp.adviceserver.strategy.impl.ChatSendSendStrategy;
import com.zyq.chirp.adviceserver.strategy.impl.NoticeSendSendStrategy;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.Session;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class RedisSubscribeListener implements MessageListener {
    NoticeSendSendStrategy noticeSendStrategy;
    ChatSendSendStrategy chatSendStrategy;
    RedisTemplate redisTemplate;
    ObjectMapper objectMapper;
    private Session session;

    public RedisSubscribeListener(Session session) {
        ApplicationContext context = SpringbootContext.getContext();
        noticeSendStrategy = context.getBean(NoticeSendSendStrategy.class);
        chatSendStrategy = context.getBean(ChatSendSendStrategy.class);
        objectMapper = context.getBean(ObjectMapper.class);
        redisTemplate = (RedisTemplate) context.getBean("redisTemplate");
        this.session = session;

    }

    public RedisSubscribeListener() {
        ApplicationContext context = SpringbootContext.getContext();
        noticeSendStrategy = context.getBean(NoticeSendSendStrategy.class);
        redisTemplate = (RedisTemplate) context.getBean("redisTemplate");
    }

    @Override
    public void onMessage(@NotNull Message message, byte[] pattern) {
        if (session != null && session.isOpen()) {
            try {
                Map<String, String> map = objectMapper.readValue(message.getBody(), new TypeReference<Map<String, String>>() {
                });
                List<NotificationDto> notice = new ArrayList<>();
                List<ChatDto> chatDtos = new ArrayList<>();
                map.forEach((type, siteMessageStr) -> {
                    try {
                        if (MessageTypeEnum.CHAT.name().equals(type)) {
                            List<ChatDto> chatDtoList = objectMapper.readValue(siteMessageStr, new TypeReference<List<ChatDto>>() {
                            });
                            chatDtos.addAll(chatDtoList);
                        }
                        if (MessageTypeEnum.NOTICE.name().equals(type)) {
                            List<NotificationDto> notificationDtos = objectMapper.readValue(siteMessageStr, new TypeReference<List<NotificationDto>>() {
                            });
                            notice.addAll(notificationDtos);
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                });
                if (!notice.isEmpty()) {
                    noticeSendStrategy.send(notice, List.of(session));
                }
                if (!chatDtos.isEmpty()) {
                    chatSendStrategy.send(chatDtos, List.of(session));
                }
            } catch (Exception e) {
                log.info("发送消息异常,e={}", e);
            }
        }
    }
}
