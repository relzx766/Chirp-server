package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceclient.enums.NoticeType;
import com.zyq.chirp.adviceserver.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
@Slf4j
public class UserMessageAssembler {
    @Resource
    NotificationService notificationService;
    @Resource
    KafkaTemplate<String, NotificationDto> kafkaTemplate;
    @Value("${mq.topic.site-message.notice}")
    String notice;

    @KafkaListener(topics = "${mq.topic.site-message.follow}",
            groupId = "${mq.consumer.group.pre-interaction}",
            batch = "true")
    public void receiver(@Payload List<NotificationDto> messageDtos, Acknowledgment ack) {
        messageDtos.forEach(messageDto -> {
            messageDto.setId(IdWorker.getId());
            messageDto.setNoticeType(NoticeType.USER.name());
            messageDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
            kafkaTemplate.send(notice, messageDto);
        });
        ack.acknowledge();
    }
}
