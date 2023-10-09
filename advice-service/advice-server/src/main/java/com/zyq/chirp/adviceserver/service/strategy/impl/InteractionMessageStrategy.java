package com.zyq.chirp.adviceserver.service.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.config.KafkaContainerConfig;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
import com.zyq.chirp.adviceserver.service.strategy.MessageStrategy;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class InteractionMessageStrategy implements MessageStrategy {

    @Resource
    KafkaContainerConfig<SiteMessageDto> kafkaContainerConfig;
    @Resource
    ObjectMapper objectMapper;
    @Resource
    ChirperAssembleImpl chirperAssemble;
    @Resource
    UserAssembleImpl userAssemble;
    @Resource
    InteractionMessageService interactionMessageService;
    @Resource
    Map<String, List<KafkaMessageListenerContainer>> containerMap;
    @Value("${mq.topic.site-message.interaction}")
    String interaction;
    @Value("${mq.consumer.group.user}")
    String group;
    @Value("${mq.topic.site-message.tweeted-advice}")
    String tweeted;

    /**
     * 推送站内信
     *
     * @param messageDtos 站内信
     * @param session     websocket session
     * @param userId      用户id
     */

    @SneakyThrows
    public void send(List<SiteMessageDto> messageDtos, Session session, Long userId) {
        if (!messageDtos.isEmpty()) {
            session.getBasicRemote().sendText(messageCombine(messageDtos));
        }
        KafkaMessageListenerContainer<String, SiteMessageDto> interactionContainer = kafkaContainerConfig.getListenerContainer(
                userId.toString(),
                interaction + "-" + userId,
                STR. "\{ group }-\{ userId }" ,
                messageHandler(session, false));
        KafkaMessageListenerContainer<String, SiteMessageDto> tweetedContainer = kafkaContainerConfig.getListenerContainer(
                userId.toString(),
                STR. "\{ tweeted }-\{ userId }" , STR. "\{ group }-\{ userId }" , messageHandler(session, true));
        containerMap.put(userId.toString(), List.of(interactionContainer, tweetedContainer));

        interactionContainer.start();
        interactionContainer.resume();
        tweetedContainer.start();
        tweetedContainer.resume();
    }


    private String messageCombine(List<SiteMessageDto> messageDtos) {
        try {
            return objectMapper.writeValueAsString(interactionMessageService.combine(messageDtos));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "[]";
    }

    private BatchMessageListener<String, SiteMessageDto> messageHandler(Session session, Boolean ack) {

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
                if (ack) {
                    acknowledgment.acknowledge();
                }
            }
        };
    }
}
