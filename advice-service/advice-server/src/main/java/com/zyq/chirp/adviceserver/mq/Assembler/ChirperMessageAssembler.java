package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.userclient.client.UserClient;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 接收上游不完整的点赞信息，并完整点赞信息发送给下游
 */

@Component
@Slf4j
public class ChirperMessageAssembler {
    @Resource
    ChirperClient chirperClient;
    @Resource
    UserClient userClient;
    @Resource
    KafkaTemplate<String, SiteMessageDto> kafkaTemplate;
    @Value("${mq.topic.site-message.interaction}")
    String interactionTopic;

    @KafkaListener(topics = {"${mq.topic.site-message.like}", "${mq.topic.site-message.forward}",
            "${mq.topic.site-message.quote}", "${mq.topic.site-message.reply}"},
            groupId = "${mq.consumer.group.pre-interaction}",
            batch = "true", concurrency = "4")
    public void receiver(@Payload List<SiteMessageDto> messageDtos, Acknowledgment ack) {
        List<Long> chirperIds = messageDtos.stream().map(SiteMessageDto::getTargetId).toList();
        if (!chirperIds.isEmpty()) {
            Map<Long, ChirperDto> chirperDtoMap;
            List<ChirperDto> chirperDtoList = chirperClient.getShort(chirperIds).getBody();
            if (chirperDtoList != null && !chirperDtoList.isEmpty()) {
                chirperDtoMap = chirperDtoList.stream()
                        .collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
                //转换为详细信息后发送
                messageDtos.forEach(siteMessage -> {
                    siteMessage.setId(IdWorker.getId());
                    siteMessage.setReceiverId(chirperDtoMap.get(siteMessage.getTargetId()).getAuthorId());
                    siteMessage.setCreateTime(new Timestamp(System.currentTimeMillis()));
                    log.info("互动消息组装完成,类型为{},发送者为{},接收者为{}", siteMessage.getType(), siteMessage.getSenderId(), siteMessage.getReceiverId());
                    kafkaTemplate.send(interactionTopic + "-" + siteMessage.getReceiverId(), siteMessage);
                });
            }
        }
        ack.acknowledge();
    }

}
