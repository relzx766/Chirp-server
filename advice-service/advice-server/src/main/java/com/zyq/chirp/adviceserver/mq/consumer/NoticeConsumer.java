package com.zyq.chirp.adviceserver.mq.consumer;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.convertor.MessageConvertor;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class NoticeConsumer {
    @Resource
    protected KafkaTemplate<String, SiteMessageDto> kafkaTemplate;
    @Resource
    InteractionMessageService interactionMessageService;
    @Resource
    MessageConvertor messageConvertor;
    @Value("${mq.topic.site-message.interaction}")
    String interactionTopic;

    @KafkaListener(topicPattern = "#{'${mq.topic.site-message.interaction}'+'-.*'}",
            groupId = "${mq.consumer.group.interaction}",
            batch = "true", concurrency = "4")
    public void receiver(@Payload List<SiteMessageDto> messageDtos, Acknowledgment ack) {
        log.info("消费到互动消息，开始准备写入数据库");
        List<Notification> notifications = messageDtos.stream()
                .map(messageDto -> {
                    Notification notification = messageConvertor.dtoToPojo(messageDto);
                    notification.setIsRead(false);
                    notification.setStatus(true);
                    return notification;
                })
                .toList();
        interactionMessageService.saveBatch(notifications);
        log.info("写入完成，开始提交偏移量");
        ack.acknowledge();
        log.info("偏移量提交完成");
    }
}
