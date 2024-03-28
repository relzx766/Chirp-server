package com.zyq.chirp.adviceserver.mq.consumer;

import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.service.ChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ChatMessageConsumer {
    @Resource
    ChatService messageService;

    @KafkaListener(topics = "${mq.topic.site-message.chat}",
            groupId = "${mq.consumer.group.chat}",
            batch = "true", concurrency = "4")
    public void receiver(@Payload List<ChatDto> messageDtos, Acknowledgment ack) {
        try {
            messageService.addBatch(messageDtos);
        } catch (Exception e) {
            log.error("写入私信失败,错误=>", e);
        } finally {
            ack.acknowledge();
        }

    }
}
