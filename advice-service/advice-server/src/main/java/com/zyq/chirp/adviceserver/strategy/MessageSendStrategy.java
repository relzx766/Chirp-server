package com.zyq.chirp.adviceserver.strategy;

import com.zyq.chirp.adviceclient.dto.SiteMessage;
import jakarta.websocket.Session;

import java.util.Collection;
import java.util.List;

public interface MessageSendStrategy<T extends SiteMessage> {

    void send(List<T> messageDtos, Collection<Session> sessions);
}
