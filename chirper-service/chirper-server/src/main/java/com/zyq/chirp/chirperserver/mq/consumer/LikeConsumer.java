package com.zyq.chirp.chirperserver.mq.consumer;

import com.zyq.chirp.chirperserver.service.LikeService;
import com.zyq.chirp.common.mq.enums.DefaultOperation;
import com.zyq.chirp.common.mq.model.Action;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LikeConsumer {
    @Resource
    LikeService likeService;
    @Value("${mq.topic.chirper.like.count}")
    String LIKE_INCREMENT_COUNT_TOPIC;
    @Value("${mq.topic.chirper.like.record}")
    String LIKE_RECORD_TOPIC;

    @KafkaListener(topics = "${mq.topic.chirper.like.record}",
            groupId = "${mq.consumer.group.like}",
            batch = "true", concurrency = "4")
    public void likeRecordConsumer(@Payload List<Action<Long, Long>> actions, Acknowledgment ack) {
        log.info("消费到主题:{}", LIKE_RECORD_TOPIC);
        Map<String, List<Action<Long, Long>>> collect = actions.stream().collect(Collectors.groupingBy(Action::getOperation));
        collect.forEach((operation, actionList) -> {
            if (DefaultOperation.INSET.getOperation().equals(operation)) {
                likeService.saveLike(actionList);
            }
            if (DefaultOperation.DELETE.getOperation().equals(operation)) {
                likeService.saveLikeCancel(actionList);
            }
        });
        ack.acknowledge();
        log.info("主题:#{}偏移量提交", LIKE_RECORD_TOPIC);
    }

    @KafkaListener(topics = "${mq.topic.chirper.like.count}",
            groupId = "${mq.consumer.group.like}",
            batch = "true", concurrency = "4")
    public void forwardCountConsumer(@Payload List<Action<Long, Long>> actions, Acknowledgment ack) {
        log.info("消费到主题:{}", LIKE_INCREMENT_COUNT_TOPIC);
        likeService.modifyLikeCount(actions);
        ack.acknowledge();
        log.info("主题:#{}偏移量提交", LIKE_INCREMENT_COUNT_TOPIC);
    }
}
