package com.zyq.chirp.feedserver.mq.consumer;

import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.common.mq.model.Message;
import com.zyq.chirp.feedserver.service.FeedService;
import com.zyq.chirp.userclient.dto.RelationDto;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户取关时，修改用户feed流
 */
@Component
public class UnfollowConsumer {
    @Resource
    FeedService feedService;
    @Resource
    ChirperClient chirperClient;
    @Value("${mq.retry.max}")
    Integer maxRetryTimes;
    @Resource
    KafkaTemplate<String, Message<RelationDto>> kafkaTemplate;

    @KafkaListener(topics = "${mq.topic.unfollow}",
            batch = "true", concurrency = "4")
    public void receiver(@Payload List<ConsumerRecord<String, Message<RelationDto>>> records, Acknowledgment ack) {
        try {
            List<Long> authors = records.stream().map(record -> record.value().getBody().getToId()).toList();
            Map<Long, List<Long>> chirperMap = chirperClient.getIdByAuthor(authors).getBody();
            if (chirperMap != null && !chirperMap.isEmpty()) {
                records.forEach(record -> {
                    Thread.ofVirtual().start(() -> {
                        Message<RelationDto> message = record.value();
                        try {
                            RelationDto relationDto = message.getBody();
                            List<Long> contentIds = chirperMap.get(relationDto.getToId());
                            if (contentIds != null && !contentIds.isEmpty()) {
                                List<String> contents = contentIds.stream().map(String::valueOf).toList();
                                feedService.removeBatch(relationDto.getFromId().toString(), contents);
                            }
                        } catch (Exception e) {
                            if (message.getRetryTimes() < maxRetryTimes) {
                                message.setRetryTimes(message.getRetryTimes() + 1);
                                kafkaTemplate.send(record.topic(), message);
                            }
                        }
                    });
                });
            }
        } finally {
            ack.acknowledge();
        }
    }

}
