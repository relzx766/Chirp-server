package com.zyq.chirp.adviceserver.mq.Assembler;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.NoticeType;
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
    @Value("${mq.topic.site-message.notice}")
    String notice;
    @Resource
    ObjectMapper objectMapper;

    @KafkaListener(topics = {"${mq.topic.site-message.like}", "${mq.topic.site-message.forward}",
            "${mq.topic.site-message.quote}", "${mq.topic.site-message.reply}", "${mq.topic.site-message.mentioned}"},
            groupId = "${mq.consumer.group.pre-interaction}",
            batch = "true", concurrency = "4")
    public void receiver(@Payload List<SiteMessageDto> messageDtos, Acknowledgment ack) {
        List<Long> chirperIds = messageDtos.stream().map(messageDto -> Long.parseLong(messageDto.getSonEntity())).toList();
        if (!chirperIds.isEmpty()) {
            Map<Long, ChirperDto> chirperDtoMap;
            List<ChirperDto> chirperDtoList = chirperClient.getBasicInfo(chirperIds).getBody();
            if (chirperDtoList != null && !chirperDtoList.isEmpty()) {
                chirperDtoMap = chirperDtoList.stream()
                        .collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
                //转换为详细信息后发送
                for (SiteMessageDto messageDto : messageDtos) {
                    messageDto.setId(IdWorker.getId());
                    if (messageDto.getReceiverId() == null) {
                        Long receiver = chirperDtoMap.get(Long.parseLong(messageDto.getSonEntity())).getAuthorId();
                        messageDto.setReceiverId(receiver);
                    }
                    //过滤掉自己给自己的互动消息
                    if (messageDto.getSenderId().equals(messageDto.getReceiverId())) {
                        continue;
                    }
                    messageDto.setNoticeType(NoticeType.USER.name());
                    messageDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
                    kafkaTemplate.send(notice, messageDto);
                }
            }
        }
        ack.acknowledge();
    }

}
