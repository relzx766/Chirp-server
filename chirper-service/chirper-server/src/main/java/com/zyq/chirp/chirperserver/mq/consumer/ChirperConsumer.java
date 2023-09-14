package com.zyq.chirp.chirperserver.mq.consumer;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.enums.ChirperStatus;
import com.zyq.chirp.chirperserver.service.ChirperService;
import com.zyq.chirp.common.model.DelayMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChirperConsumer {
    @Resource
    ChirperService chirperService;

    /**
     * 消费延时发布的推文
     *
     * @param delayMessage
     * @param ack
     */
    // @KafkaListener(topics = "${mq.topic.chirper-delay-post}",groupId = "${spring.kafka.consumer.group-id}")
    public void delayPostConsumer(@Payload DelayMessage delayMessage, Acknowledgment ack) {
        boolean isDue = System.currentTimeMillis() - delayMessage.getCreateTime() - delayMessage.getDelayTime().toMillis() >= 0;
        if (isDue) {
            ChirperDto chirperDto = (ChirperDto) delayMessage.getData();
            chirperService.updateStatus(chirperDto.getId(), ChirperStatus.ACTIVE);
            log.info("推文{}到达指定的发布时间，更改其状态为{}", chirperDto.getId(), ChirperStatus.ACTIVE);
            ack.acknowledge();
        }
    }
}
