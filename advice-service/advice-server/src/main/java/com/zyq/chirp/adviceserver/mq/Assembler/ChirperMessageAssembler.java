package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEntityTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeEventTypeEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeStatusEnums;
import com.zyq.chirp.adviceserver.domain.enums.NoticeTypeEnums;
import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 接收上游不完整的点赞信息，并完整点赞信息发送给下游
 */

@Component
@Slf4j
public class ChirperMessageAssembler {
    @Resource
    ChirperClient chirperClient;
    @Resource
    KafkaTemplate<String, NotificationDto> kafkaTemplate;
    @Value("${mq.topic.site-message.notice}")
    String notice;
    @Value("${mq.topic.site-message.like}")
    String likeTopic;
    @Value("${mq.topic.site-message.forward}")
    String forwardTopic;
    @Value("${mq.topic.site-message.quote}")
    String quoteTopic;
    @Value("${mq.topic.site-message.reply}")
    String replyTopic;
    @Value("${mq.topic.site-message.mentioned}")
    String mentionedTopic;

    @KafkaListener(topics = {"${mq.topic.site-message.like}", "${mq.topic.site-message.forward}",
            "${mq.topic.site-message.quote}", "${mq.topic.site-message.reply}", "${mq.topic.site-message.mentioned}"},
            groupId = "${mq.consumer.group.pre-interaction}",
            batch = "true", concurrency = "4")
    public void receiver(@Payload List<ConsumerRecord<String, NotificationDto>> records, Acknowledgment ack) {
        try {
            List<Long> chirperIds = records.stream()
                    .map(ConsumerRecord::value)
                    .map(NotificationDto::getSonEntity)
                    .map(Long::parseLong)
                    .toList();
            if (!chirperIds.isEmpty()) {
                ResponseEntity<List<ChirperDto>> response = chirperClient.getBasicInfo(chirperIds);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Map<Long, ChirperDto> chirperDtoMap = Objects.requireNonNull(response.getBody())
                            .stream().collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
                    for (ConsumerRecord<String, NotificationDto> record : records) {
                        String topic = record.topic();
                        NotificationDto notificationDto = record.value();
                        notificationDto.setId(IdWorker.getId());
                        notificationDto.setEntityType(NoticeEntityTypeEnums.CHIRPER.name());
                        notificationDto.setNoticeType(NoticeTypeEnums.USER.name());
                        notificationDto.setStatus(NoticeStatusEnums.UNREAD.getStatus());
                        notificationDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
                        if (notificationDto.getReceiverId() == null) {
                            ChirperDto chirperDto = chirperDtoMap.get(Long.parseLong(notificationDto.getSonEntity()));
                            if (chirperDto == null || notificationDto.getSenderId().equals(chirperDto.getAuthorId())) {
                                continue;
                            } else {
                                notificationDto.setReceiverId(chirperDto.getAuthorId());
                            }
                        }
                        if (likeTopic.equalsIgnoreCase(topic)) {
                            notificationDto.setEvent(NoticeEventTypeEnums.LIKE.name());
                        } else if (forwardTopic.equalsIgnoreCase(topic)) {
                            notificationDto.setEvent(NoticeEventTypeEnums.FORWARD.name());
                        } else if (replyTopic.equalsIgnoreCase(topic)) {
                            notificationDto.setEvent(NoticeEventTypeEnums.REPLY.name());
                        } else if (quoteTopic.equalsIgnoreCase(topic)) {
                            notificationDto.setEvent(NoticeEventTypeEnums.QUOTE.name());
                        } else if (mentionedTopic.equalsIgnoreCase(topic)) {
                            notificationDto.setEvent(NoticeEventTypeEnums.MENTIONED.name());
                        }
                        kafkaTemplate.send(notice, notificationDto);
                    }
                } else if (response.getStatusCode().isError()) {
                    log.error("组装推文互动消息时获取推文信息错误，组装失败=>{}", response);
                }
            }
        } catch (NumberFormatException e) {
            log.error("组装推文互动消息失败，错误=>", e);
        } finally {
            ack.acknowledge();
        }

    }

}
