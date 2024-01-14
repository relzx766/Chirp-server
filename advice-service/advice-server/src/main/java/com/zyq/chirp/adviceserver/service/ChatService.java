package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.ChatDto;
import jakarta.websocket.Session;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ChatService {
    void send(ChatDto chatDto);

    Boolean canSendChat(Long sender, Long receiver);

    void add(ChatDto chatDto);

    void connect(Long userId, Session session);

    void disconnect(Long userId, Session session);

    void addBatch(Collection<ChatDto> chatDtos);

    /**
     * 缓存每个话题下最新的部分消息
     *
     * @param chatList 消息列表
     */
    void cacheChatByScore(Collection<ChatDto> chatList);

    /**
     * 在缓存中获取话题最新部分
     */
    List<Long> getConvTop(Collection<String> conversations);

    List<ChatDto> getChatIndex(Long userId);

    void deleteById(Long messageId);

    void markAsDel(Long messageId, Long userId);

    void markAsDel(String conversationId, Long userId);

    List<ChatDto> getById(Collection<Long> messageIds);

    Map<String, List<ChatDto>> getChatByConversation(Collection<String> conversations, Long userId, Integer page);

    List<ChatDto> getChatHistory(Long currentUserId, Long otherUserId, Integer page);

    List<ChatDto> getChatHistory(String conversationId, Long userId, Integer page, Integer size);

    List<ChatDto> getReference(List<ChatDto> chatDtos);

    Map<String, Integer> getUnreadCount(Collection<String> conversation, Long receiverId);

    void markAsRead(Collection<String> conversationIds, Long receiverId);

    Set<String> getConversationByUserId(Long userId);

}
