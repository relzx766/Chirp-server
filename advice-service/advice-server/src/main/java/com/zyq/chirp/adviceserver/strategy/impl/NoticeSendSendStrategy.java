package com.zyq.chirp.adviceserver.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceclient.enums.MessageTypeEnum;
import com.zyq.chirp.adviceserver.exception.SendFailedException;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import com.zyq.chirp.adviceserver.strategy.MessageSendStrategy;
import com.zyq.chirp.common.domain.model.Code;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NoticeSendSendStrategy implements MessageSendStrategy<NotificationDto> {

    @Resource
    ObjectMapper objectMapper;


    @Value("${mq.topic.site-message.interaction}")
    String interaction;
    @Value("${mq.consumer.group.user}")
    String group;
    @Value("${mq.topic.site-message.tweeted-advice}")
    String tweeted;
    @Resource
    MessageAssembleStrategy<NotificationDto> assembleStrategy;

    /**
     * 推送站内信
     *
     * @param messageDtos 站内信
     * @param sessions    websocket session
     */

    public void send(List<NotificationDto> messageDtos, Collection<Session> sessions) {
        assembleStrategy.assemble(messageDtos);
        sessions.forEach(session -> {
            try {
                session.getAsyncRemote().sendText(objectMapper.writeValueAsString(Map.of(MessageTypeEnum.NOTICE.name(), messageDtos)));
            } catch (IOException e) {
                throw new SendFailedException(e.getMessage(), e.getCause(), Code.ERR_SYSTEM.getCode(), messageDtos);
            }
        });

    }

}
