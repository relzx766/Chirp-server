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
public class ForwardConsumer {
    @Resource
    ChirperService chirperService;
    @Value("${mq.topic.chirper.forward.record}")
    String FORWARD_RECORD_TOPIC;
    @Value("${mq.topic.chirper.forward.count}")
    String FORWARD_INCREMENT_COUNT_TOPIC;

    /*    @KafkaListener(topics = "${mq.topic.chirper.forward.record}",
                groupId = "${mq.consumer.group.forward}",
                batch = "true", concurrency = "4")
        public void forwardRecordConsumer(@Payload List<Action<Long,Long>>actions, Acknowledgment ack){
            log.info("消费到主题:{}", FORWARD_RECORD_TOPIC);
            Map<String, List<Action<Long, Long>>> collect = actions.stream().collect(Collectors.groupingBy(Action::getOperation));
            collect.forEach(((operation, actionList) -> {
                if (DefaultOperation.INSET.getOperation().equals(operation)){
                    chirperService.saveForward(actionList);
                }
                if (DefaultOperation.DELETE.getOperation().equals(operation)){
                    chirperService.saveForwardCancel(actionList);
                }
            }));
            ack.acknowledge();
            log.info("主题:#{}偏移量提交",FORWARD_RECORD_TOPIC);
        }*/
    @KafkaListener(topics = "${mq.topic.chirper.forward.count}",
            groupId = "${mq.consumer.group.forward}",
            batch = "true", concurrency = "4")
    public void forwardCountConsumer(@Payload List<Action<Long, Long>> actions, Acknowledgment ack) {
        log.info("消费到主题:{}", FORWARD_INCREMENT_COUNT_TOPIC);
        chirperService.modifyForwardCount(actions);
        ack.acknowledge();
        log.info("主题:#{}偏移量提交", FORWARD_INCREMENT_COUNT_TOPIC);
    }
}
