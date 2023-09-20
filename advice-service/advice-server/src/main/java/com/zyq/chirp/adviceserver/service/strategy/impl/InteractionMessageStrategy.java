package com.zyq.chirp.adviceserver.service.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.EntityType;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.config.KafkaContainerConfig;
import com.zyq.chirp.adviceserver.service.strategy.MessageStrategy;
import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InteractionMessageStrategy implements MessageStrategy {

    @Resource
    KafkaListenerEndpointRegistry registry;
    @Resource
    KafkaContainerConfig<SiteMessageDto> kafkaContainerConfig;
    @Resource
    ObjectMapper objectMapper;
    @Resource
    ChirperClient chirperClient;
    @Resource
    UserClient userClient;
    @Resource
    Map<String, KafkaMessageListenerContainer> containerMap;
    @Value("${mq.topic.site-message.interaction}")
    String topicSuffix;
    @Value("${mq.consumer.group.interaction}")
    String group;

    /**
     * 推送站内信
     *
     * @param messageDtos 站内信
     * @param session     websocket session
     * @param userId      用户id
     */

    @SneakyThrows
    public void send(List<SiteMessageDto> messageDtos, Session session, Long userId) {
        KafkaMessageListenerContainer<String, SiteMessageDto> container = kafkaContainerConfig.getListenerContainer(userId.toString(),
                topicSuffix + "-" + userId, group, messageHandler(session));
        containerMap.put(userId.toString(), container);
        if (!messageDtos.isEmpty()) {
            session.getBasicRemote().sendText(messageCombine(messageDtos));
        }
        container.start();
        container.resume();
    }

    private List<SiteMessageDto> chirperMessageCombine(List<SiteMessageDto> messageDtos) {
        try {
            List<Long> chirperIds = new ArrayList<>();
            List<Long> senderIds = new ArrayList<>();
            messageDtos.forEach(messageDto -> {
                Long sonEntityId = messageDto.getSonEntity() != null ? Long.parseLong(messageDto.getSonEntity()) : null;
                Long entityId = messageDto.getEntity() != null ? Long.parseLong(messageDto.getEntity()) : null;
                chirperIds.add(sonEntityId);
                chirperIds.add(entityId);
                senderIds.add(messageDto.getSenderId());
            });
            CompletableFuture<List<ChirperDto>> chirperFuture = CompletableFuture.supplyAsync(() ->
                    chirperClient.getContent(chirperIds).getBody()
            ).exceptionally(throwable -> {
                throwable.printStackTrace();
                return List.of();
            });
            CompletableFuture<List<UserDto>> userFuture = CompletableFuture.supplyAsync(()
                    -> userClient.getShort(senderIds).getBody()
            ).exceptionally(throwable -> {
                throwable.printStackTrace();
                return List.of();
            });
            CompletableFuture<Void> allOf = CompletableFuture.allOf(chirperFuture, userFuture);
            allOf.join();
            Map<Long, ChirperDto> chirperMap = chirperFuture.get()
                    .stream().collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
            Map<Long, UserDto> userMap = userFuture.get()
                    .stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
            messageDtos.forEach(messageDto -> {
                try {
                    Long sonEntityId = messageDto.getSonEntity() != null ? Long.parseLong(messageDto.getSonEntity()) : null;
                    Long entityId = messageDto.getEntity() != null ? Long.parseLong(messageDto.getEntity()) : null;
                    messageDto.setEntity(objectMapper.writeValueAsString(chirperMap.get(entityId)));
                    messageDto.setSonEntity(objectMapper.writeValueAsString(chirperMap.get(sonEntityId)));
                    UserDto sender = userMap.get(messageDto.getSenderId());
                    messageDto.setSenderName(sender.getNickname());
                    messageDto.setSenderAvatar(sender.getSmallAvatarUrl());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return messageDtos;
    }

    private List<SiteMessageDto> userMessageCombine(List<SiteMessageDto> messageDtos) {
        try {
            List<Long> senderIds = messageDtos.stream().map(SiteMessageDto::getSenderId).toList();
            Map<Long, UserDto> senderMap = Objects.requireNonNull(userClient.getShort(senderIds).getBody())
                    .stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
            messageDtos.forEach(messageDto -> {
                UserDto sender = senderMap.get(messageDto.getSenderId());
                messageDto.setSenderName(sender.getNickname());
                messageDto.setSenderAvatar(sender.getSmallAvatarUrl());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messageDtos;
    }

    private String messageCombine(List<SiteMessageDto> messageDtos) {
        List<SiteMessageDto> chirperMsg = new ArrayList<>();
        List<SiteMessageDto> userMsg = new ArrayList<>();
        messageDtos.forEach(messageDto -> {
            if (EntityType.CHIRPER.name().equals(messageDto.getEntityType())) {
                chirperMsg.add(messageDto);
            }
            if (EntityType.USER.name().equals(messageDto.getEntityType())) {
                userMsg.add(messageDto);
            }
        });
        if (!chirperMsg.isEmpty()) {
            this.chirperMessageCombine(chirperMsg);
        }
        if (!userMsg.isEmpty()) {
            this.userMessageCombine(userMsg);
        }
        List<SiteMessageDto> messages = new ArrayList<>();
        messages.addAll(chirperMsg);
        messages.addAll(userMsg);
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "[]";
    }

    private BatchMessageListener<String, SiteMessageDto> messageHandler(Session session) {

        return new BatchAcknowledgingMessageListener<String, SiteMessageDto>() {
            @SneakyThrows
            @Override
            public void onMessage(List<ConsumerRecord<String, SiteMessageDto>> data, Acknowledgment acknowledgment) {
                List<SiteMessageDto> siteMessageDtos = data.stream()
                        .map(ConsumerRecord::value)
                        .toList();
                if (!siteMessageDtos.isEmpty()) {
                    String message = messageCombine(siteMessageDtos);
                    session.getBasicRemote().sendText(message);
                }
            }
        };
    }
}
