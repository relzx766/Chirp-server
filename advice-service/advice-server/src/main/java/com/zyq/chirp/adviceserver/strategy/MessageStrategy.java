package com.zyq.chirp.adviceserver.strategy;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import jakarta.websocket.Session;

import java.util.List;

public interface MessageStrategy {

    void send(List<SiteMessageDto> messageDtos, List<Session> sessions);
}
