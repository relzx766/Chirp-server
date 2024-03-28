package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEntityTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEventTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeStatusEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeTypeEnums;
import com.zyq.chirp.adviceserver.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserMessageAssembler {
    @Resource
    KafkaTemplate<String, NotificationDto> kafkaTemplate;
    @Value("${mq.topic.site-message.notice}")
    String notice;
    @Value("${mq.topic.site-message.follow}")
    String followTopic;
    @Resource
    NotificationService notificationService;
    @KafkaListener(topics = "${mq.topic.site-message.follow}",
            groupId = "${mq.consumer.group.pre-interaction}",
            batch = "true")
    public void receiver(@Payload List<ConsumerRecord<String, NotificationDto>> records, Acknowledgment ack) {
        try {
            List<NotificationDto> notificationDtos = new ArrayList<>();
            for (ConsumerRecord<String, NotificationDto> record : records) {
                String topic = record.topic();
                NotificationDto notificationDto = record.value();
                notificationDto.setId(IdWorker.getId());
                notificationDto.setNoticeType(NoticeTypeEnums.USER.name());
                notificationDto.setEntityType(NoticeEntityTypeEnums.USER.name());
                notificationDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
                notificationDto.setStatus(NoticeStatusEnums.UNREAD.getStatus());
                if (followTopic.equalsIgnoreCase(topic)) {
                    notificationDto.setEvent(NoticeEventTypeEnums.FOLLOW.name());
                }
                notificationDtos.add(notificationDto);
            }
            notificationDtos = notificationService.getSendable(notificationDtos);
            for (NotificationDto notificationDto : notificationDtos) {
                if (NoticeStatusEnums.UNREACHABLE.getStatus() != notificationDto.getStatus()) {
                    kafkaTemplate.send(notice, notificationDto);
                }
            }
        } catch (Exception e) {
            log.error("组装用户互动消息失败，错误=>", e);
        } finally {
            ack.acknowledge();
        }

    }
}
