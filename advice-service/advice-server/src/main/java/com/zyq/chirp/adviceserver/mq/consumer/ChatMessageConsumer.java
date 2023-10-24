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
        log.info("消费到私信，开始写入数据库");
        try {
            messageService.addBatch(messageDtos);
            log.info("写入完成");
        } catch (Exception e) {
            log.warn("写入失败");
        } finally {
            ack.acknowledge();
        }

    }
}
