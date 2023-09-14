package com.zyq.chirp.adviceserver.controller;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.InteractionMessageService;
import com.zyq.chirp.adviceserver.service.strategy.context.MessageContext;
import com.zyq.chirp.adviceserver.service.strategy.impl.InteractionMessageStrategy;
import jakarta.annotation.Resource;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/interaction/{userId}")
@Component
@Slf4j
public class InteractionController {
    static InteractionMessageStrategy messageStrategy;
    static InteractionMessageService interactionMessageService;
    static Map<String, KafkaMessageListenerContainer> containerMap;

    private static ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();
    private static CopyOnWriteArraySet<InteractionController> webSocketSet = new CopyOnWriteArraySet<>();

    @Autowired
    public void setMessageStrategy(InteractionMessageStrategy messageStrategy) {
        InteractionController.messageStrategy = messageStrategy;
    }

    @Autowired

    public void setInteractionMessageService(InteractionMessageService interactionMessageService) {
        InteractionController.interactionMessageService = interactionMessageService;
    }

    @Resource
    public void setContainerMap(Map<String, KafkaMessageListenerContainer> containerMap) {
        InteractionController.containerMap = containerMap;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        log.info("WebSocket建立连接中,连接用户ID：{}", userId);
        webSocketSet.add(this);
        sessionPool.put(userId, session);
        List<SiteMessageDto> messages = interactionMessageService.getByReceiverId(userId);
        System.out.println(messages);
        new MessageContext(messageStrategy).send(messages, session, userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        log.info("WebSocket断开连接,用户ID：{}", userId);
        webSocketSet.remove(this);
        MessageListenerContainer container = containerMap.get(userId.toString());
        if (container != null && container.isRunning()) {
            System.out.println("销毁kafka容器" + userId);
            container.stop();
            container.destroy();
        }
    }
}
