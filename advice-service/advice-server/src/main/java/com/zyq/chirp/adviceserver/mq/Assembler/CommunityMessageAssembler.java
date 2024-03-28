package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEntityTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEventTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeStatusEnums;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CommunityMessageAssembler {
    @Value("${mq.topic.community.invite}")
    String TOPIC_INVITE;
    @Value("${mq.topic.site-message.notice}")
    String TOPIC_NOTICE;
    @Resource
    NotificationService notificationService;
    @Resource
    KafkaTemplate<String, NotificationDto> kafkaTemplate;

    @KafkaListener(topics = {"${mq.topic.community.invite}"}, groupId = "${mq.consumer.group.community}", batch = "true", concurrency = "2")
    public void receiver(@Payload List<ConsumerRecord<String, NotificationDto>> records, Acknowledgment ack) {
        try {
            List<NotificationDto> notificationDtos = new ArrayList<>();
            for (ConsumerRecord<String, NotificationDto> record : records) {
                String topic = record.topic();
                NotificationDto notificationDto = record.value();
                notificationDto.setId(IdWorker.getId());
                notificationDto.setStatus(NoticeStatusEnums.UNREAD.getStatus());
                notificationDto.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
                if (TOPIC_INVITE.equals(topic)) {
                    notificationDto.setEvent(NoticeEventTypeEnums.COMMUNITY_INVITATION.name());
                    notificationDto.setEntityType(NoticeEntityTypeEnums.COMMUNITY_INVITATION.name());
                }
                notificationDtos.add(notificationDto);
            }
            notificationDtos = notificationService.getSendable(notificationDtos);
            for (NotificationDto notificationDto : notificationDtos) {
                if (NoticeStatusEnums.UNREACHABLE.getStatus() != notificationDto.getStatus()) {
                    kafkaTemplate.send(TOPIC_NOTICE, notificationDto);
                }
            }
        } catch (Exception e) {
            log.error("组装社群相关消息失败", e);
        } finally {
            ack.acknowledge();
        }
    }
}
