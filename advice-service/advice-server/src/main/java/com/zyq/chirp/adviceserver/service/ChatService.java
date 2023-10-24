package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.ChatDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ChatService {
    void send(ChatDto chatDto);

    void add(ChatDto chatDto);

    void addBatch(Collection<ChatDto> chatDtos);

    void deleteById(Long messageId);

    Map<String, List<ChatDto>> getChatByConversation(Collection<String> conversations, Integer page);

    List<ChatDto> getChatHistory(Long receiverId, Long senderId, Integer page);

    List<ChatDto> getChatHistory(String conversationId, Integer page, Integer size);


    Map<String, Integer> getUnreadCount(Collection<String> conversation, Long receiverId);

    void markAsRead(Collection<String> conversationIds, Long receiverId);

    Set<String> getConversationByUserId(Long userId);
}
