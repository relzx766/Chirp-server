package com.zyq.chirp.adviceserver.service;

import jakarta.websocket.Session;

public interface WsService {
    void connect(Long userId, Session session);

    void disconnect(Long userId, Session session);
}
