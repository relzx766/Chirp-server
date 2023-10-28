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

    void deleteById(Long messageId);

    List<ChatDto> getById(Collection<Long> messageIds);

    Map<String, List<ChatDto>> getChatByConversation(Collection<String> conversations, Integer page);

    List<ChatDto> getChatHistory(Long receiverId, Long senderId, Integer page);

    List<ChatDto> getChatHistory(String conversationId, Integer page, Integer size);


    Map<String, Integer> getUnreadCount(Collection<String> conversation, Long receiverId);

    void markAsRead(Collection<String> conversationIds, Long receiverId);

    Set<String> getConversationByUserId(Long userId);
}
