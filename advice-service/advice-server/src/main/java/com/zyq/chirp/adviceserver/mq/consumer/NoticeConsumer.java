package com.zyq.chirp.adviceserver.mq.consumer;

import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.convertor.NoticeConvertor;
import com.zyq.chirp.adviceserver.domain.enums.NoticeStatusEnums;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import com.zyq.chirp.adviceserver.service.NotificationService;
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
    protected KafkaTemplate<String, NotificationDto> kafkaTemplate;
    @Resource
    NotificationService notificationService;
    @Resource
    NoticeConvertor noticeConvertor;
    @Value("${mq.topic.site-message.interaction}")
    String interactionTopic;

    @KafkaListener(topics = "${mq.topic.site-message.notice}",
            groupId = "${mq.consumer.group.interaction}",
            batch = "true", concurrency = "4")
    public void receiver(@Payload List<NotificationDto> messageDtos, Acknowledgment ack) {
        try {
            List<Notification> notifications = messageDtos.stream()
                    .map(messageDto -> {
                        Notification notification = noticeConvertor.dtoToPojo(messageDto);
                        notification.setStatus(NoticeStatusEnums.UNREAD.getStatus());
                        return notification;
                    })
                    .toList();
            notificationService.saveBatch(notifications);
        } catch (Exception e) {
            log.error("写入通知失败,错误=>", e);
        } finally {
            ack.acknowledge();
        }
    }
}
