package com.zyq.chirp.chirperserver.mq.consumer;

import com.zyq.chirp.chirperserver.service.ChirperService;
import com.zyq.chirp.common.mq.model.Action;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class QuoteConsumer {
    @Resource
    ChirperService chirperService;
    @Value("${mq.topic.chirper.quote.count}")
    String QUOTE_INCREMENT_COUNT_TOPIC;

    @KafkaListener(topics = "${mq.topic.chirper.quote.count}",
            groupId = "${mq.consumer.group.quote}",
            batch = "true", concurrency = "4")
    public void quoteCountConsumer(@Payload List<Action<Long, Long>> actions, Acknowledgment ack) {
        log.info("消费到主题:{}", QUOTE_INCREMENT_COUNT_TOPIC);
        chirperService.modifyQuoteCount(actions);
        ack.acknowledge();
        log.info("主题:#{}偏移量提交", QUOTE_INCREMENT_COUNT_TOPIC);
    }
}
