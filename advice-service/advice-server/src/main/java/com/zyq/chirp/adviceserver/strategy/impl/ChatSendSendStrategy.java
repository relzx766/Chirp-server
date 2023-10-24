package com.zyq.chirp.adviceserver.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.enums.MessageTypeEnum;
import com.zyq.chirp.adviceserver.domain.enums.ChatStatusEnum;
import com.zyq.chirp.adviceserver.exception.SendFailedException;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import com.zyq.chirp.adviceserver.strategy.MessageSendStrategy;
import com.zyq.chirp.common.model.Code;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChatSendSendStrategy implements MessageSendStrategy<ChatDto> {
    @Resource
    ObjectMapper objectMapper;
    @Resource
    private MessageAssembleStrategy<ChatDto> assembleStrategy;

    @Override
    public void send(List<ChatDto> messageDtos, Collection<Session> sessions) {
        assembleStrategy.assemble(messageDtos);
        messageDtos.forEach(chatDto -> {
            chatDto.setStatus(ChatStatusEnum.UNREAD.getStatus());
        });
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(objectMapper.writeValueAsString(Map.of(MessageTypeEnum.CHAT.name(), messageDtos)));
                }
            } catch (IOException e) {
                throw new SendFailedException(e.getMessage(), e.getCause(), Code.ERR_SYSTEM.getCode(), messageDtos);
            }
        });
    }
}
