package com.zyq.chirp.adviceserver.mq.dispatcher;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.strategy.impl.SiteMessageStrategy;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NoticeDispatcher {
    ConcurrentHashMap<Long, List<Session>> userWsSession = new ConcurrentHashMap<>();
    @Resource
    SiteMessageStrategy siteMessageStrategy;

    @KafkaListener(topics = {"${mq.topic.site-message.notice}", "${mq.topic.site-message.tweeted-advice}"}, groupId = "${mq.consumer.group.notice}",
            concurrency = "10", batch = "true")
    public void dispatcher(@Payload List<ConsumerRecord<String, SiteMessageDto>> records, Acknowledgment ack) {
        Map<Long, List<SiteMessageDto>> messageMap = new HashMap<>();
        records.forEach(record -> {
            SiteMessageDto siteMessageDto = record.value();
            siteMessageDto.setOffset(record.offset());
            if (userWsSession.containsKey(siteMessageDto.getReceiverId())) {
                if (messageMap.containsKey(siteMessageDto.getReceiverId())) {
                    messageMap.get(siteMessageDto.getReceiverId()).add(siteMessageDto);
                } else {
                    messageMap.put(siteMessageDto.getReceiverId(), new ArrayList<>(Collections.singleton(siteMessageDto)));
                }
            }
        });
        messageMap.forEach((userId, messages) -> {
            siteMessageStrategy.send(messages, userWsSession.get(userId));
        });
        ack.acknowledge();
    }

    public void addSession(Long userId, Session session) {
        if (userWsSession.containsKey(userId)) {
            userWsSession.get(userId).add(session);
        } else {
            userWsSession.put(userId, new ArrayList<>(Collections.singleton(session)));
        }
    }
}
