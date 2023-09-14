package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
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
    InteractionMessageService interactionMessageService;
    @Resource
    KafkaTemplate<String, SiteMessageDto> kafkaTemplate;
    @Value("${mq.topic.site-message.interaction}")
    String interactionTopic;

    @KafkaListener(topics = "${mq.topic.site-message.follow}",
            groupId = "${mq.consumer.group.pre-interaction}",
            batch = "true")
    public void receiver(@Payload List<SiteMessageDto> messageDtos, Acknowledgment ack) {
        messageDtos.forEach(messageDto -> {
            messageDto.setId(IdWorker.getId());
            messageDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
            log.info("互动消息组装完成,类型为{},发送者为{},接收者为{}", messageDto.getType(), messageDto.getSenderId(), messageDto.getReceiverId());
            kafkaTemplate.send(interactionTopic + "-" + messageDto.getReceiverId(), messageDto);
            ack.acknowledge();
        });
    }
}
