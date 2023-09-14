package com.zyq.chirp.adviceserver.service.strategy;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import jakarta.websocket.Session;

import java.util.List;

public interface MessageStrategy {

    void send(List<SiteMessageDto> messageDtos, Session session, Long userId);
}
