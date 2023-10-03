package com.zyq.chirp.authserver.mq;

import com.zyq.chirp.authserver.service.AuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class OnlineHandler {
    @Resource
    AuthService authService;

    @KafkaListener(topics = "${mq.topic.socket-connect}",
            groupId = "${mq.consumer.group.online}",
            batch = "true", concurrency = "1")
    public void connectReceiver(@Payload List<Long> userIds, Acknowledgment ack) {
        userIds.forEach(id -> {
            authService.online(id.toString());
        });
        ack.acknowledge();
    }

    @KafkaListener(topics = "${mq.topic.socket-disconnect}",
            groupId = "${mq.consumer.group.online}",
            batch = "true", concurrency = "1")
    public void disconnectReceiver(@Payload List<Long> userIds, Acknowledgment ack) {
        userIds.forEach(id -> {
            authService.offline(id.toString());
        });
        ack.acknowledge();
    }

}
