package com.zyq.chirp.adviceserver.service.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
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
     * 推送未写入数据库的站内信
     *
     * @param messageDtos 未写入数据库的站内信
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

    private String messageCombine(List<SiteMessageDto> messageDtos) throws JsonProcessingException, ExecutionException, InterruptedException {
        //获取推文、用户id列表
        List<Long> chirperIds = messageDtos.stream().map(SiteMessageDto::getTargetId).toList();
        List<Long> userIds = messageDtos.stream().map(SiteMessageDto::getSenderId).toList();
        //异步获取推文、用户信息
        CompletableFuture<List<ChirperDto>> chirperFuture = CompletableFuture.supplyAsync(() ->
                        chirperClient.getShort(chirperIds).getBody())
                .exceptionally(throwable -> List.of());
        CompletableFuture<List<UserDto>> userFuture = CompletableFuture.supplyAsync(() ->
                        userClient.getShort(userIds).getBody())
                .exceptionally(throwable -> List.of());
        CompletableFuture<Void> combine = CompletableFuture.allOf(chirperFuture, userFuture);
        //等待信息获取完成
        combine.join();
        Map<Long, ChirperDto> chirperMap = chirperFuture.get().stream().collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
        Map<Long, UserDto> userMap = userFuture.get().stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
        messageDtos.forEach(messageDto -> {
            messageDto.setSenderAvatar(userMap.get(messageDto.getSenderId()).getSmallAvatarUrl());
            messageDto.setSenderName(userMap.get(messageDto.getSenderId()).getNickname());
            if (messageDto.getTargetId() != null) {
                ChirperDto chirperDto = chirperMap.get(messageDto.getTargetId());
                if (chirperDto != null) {
                    messageDto.setText(chirperMap.get(messageDto.getTargetId()).getText());
                }
            }
        });
        return objectMapper.writeValueAsString(messageDtos);
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
