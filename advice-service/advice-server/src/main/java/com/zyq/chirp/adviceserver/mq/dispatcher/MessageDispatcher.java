package com.zyq.chirp.adviceserver.mq.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.enums.MessageTypeEnums;
import com.zyq.chirp.authclient.client.AuthClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MessageDispatcher {
    @Resource
    RedisTemplate<String, Object> redisTemplate;
    @Value("${mq.topic.site-message.user}")
    String messageTopic;
    @Resource
    AuthClient authClient;
    @Resource
    ObjectMapper objectMapper;
    @KafkaListener(topics = {"${mq.topic.site-message.notice}", "${mq.topic.site-message.tweeted-advice}"}, groupId = "${mq.consumer.group.site-message}",
            concurrency = "10", batch = "true")
    public void noticeDispatcher(@Payload List<ConsumerRecord<String, NotificationDto>> records, Acknowledgment ack) {
        Map<String, List<NotificationDto>> messageMap = records.stream()
                .collect(Collectors.groupingBy(record -> record.value().getReceiverId().toString(),
                Collectors.mapping(ConsumerRecord::value, Collectors.toList())));
        Map<String, Boolean> onlineMap = authClient.multiCheck(messageMap.keySet()).getBody();
        if (onlineMap != null) {
            onlineMap.forEach((userId, isOnline) -> {
                if (isOnline) {
                    try {
                        redisTemplate.convertAndSend(messageTopic + userId, Map.entry(MessageTypeEnums.NOTICE.name(), objectMapper.writeValueAsString(messageMap.get(userId))));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = {"${mq.topic.site-message.chat}"}, groupId = "${mq.consumer.group.site-message}",
            concurrency = "10", batch = "true")
    public void chatDispatcher(@Payload List<ConsumerRecord<String, ChatDto>> records, Acknowledgment ack) {
        Map<String, List<ChatDto>> messageMap = records.stream().collect(Collectors.groupingBy(record -> record.value().getReceiverId().toString(),
                Collectors.mapping(ConsumerRecord::value, Collectors.toList())));
        Map<String, Boolean> onlineMap = authClient.multiCheck(messageMap.keySet()).getBody();
        if (onlineMap != null) {
            onlineMap.forEach((userId, isOnline) -> {
                if (isOnline) {
                    try {
                        redisTemplate.convertAndSend(messageTopic + userId, Map.entry(MessageTypeEnums.CHAT.name(), objectMapper.writeValueAsString(messageMap.get(userId))));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        ack.acknowledge();
    }
}
