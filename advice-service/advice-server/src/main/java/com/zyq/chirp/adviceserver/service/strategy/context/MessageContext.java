package com.zyq.chirp.adviceserver.service.strategy.context;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.strategy.MessageStrategy;
import jakarta.websocket.Session;

import java.util.List;

public class MessageContext {
    private MessageStrategy strategy;

    public MessageContext(MessageStrategy strategy) {
        this.strategy = strategy;
    }

    public void send(List<SiteMessageDto> messageDtos, Session session, Long userId) {
        this.strategy.send(messageDtos, session, userId);
    }

}
