package com.zyq.chirp.adviceserver.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.SiteMessageAssembleService;
import com.zyq.chirp.adviceserver.strategy.MessageStrategy;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class SiteMessageStrategy implements MessageStrategy {

    @Resource
    ObjectMapper objectMapper;


    @Value("${mq.topic.site-message.interaction}")
    String interaction;
    @Value("${mq.consumer.group.user}")
    String group;
    @Value("${mq.topic.site-message.tweeted-advice}")
    String tweeted;
    @Resource
    SiteMessageAssembleService assembleService;

    /**
     * 推送站内信
     *
     * @param messageDtos 站内信
     * @param sessions    websocket session
     */

    public void send(List<SiteMessageDto> messageDtos, List<Session> sessions) {
        List<SiteMessageDto> siteMessageDtos = assembleService.assemble(messageDtos);
        sessions.forEach(session -> {
            try {
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(siteMessageDtos));
            } catch (IOException e) {
                log.warn("通知发送失败{}", e.getCause());
            }
        });

    }

}
