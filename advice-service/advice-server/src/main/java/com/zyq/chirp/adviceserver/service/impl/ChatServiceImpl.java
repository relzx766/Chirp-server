package com.zyq.chirp.adviceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.convertor.ChatConvertor;
import com.zyq.chirp.adviceserver.domain.enums.ChatStatusEnum;
import com.zyq.chirp.adviceserver.domain.enums.ChatTypeEnum;
import com.zyq.chirp.adviceserver.domain.pojo.Chat;
import com.zyq.chirp.adviceserver.mapper.ChatMapper;
import com.zyq.chirp.adviceserver.service.ChatService;
import com.zyq.chirp.adviceserver.util.ChatUtil;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


@Service
public class ChatServiceImpl implements ChatService {
    @Resource
    ChatMapper messageMapper;
    @Resource
    ChatConvertor convertor;
    @Resource
    KafkaTemplate<String, ChatDto> kafkaTemplate;
    @Value("${default-config.page-size}")
    Integer pageSize;
    @Value("${mq.topic.site-message.chat}")
    String topic;

    @Override
    public void send(ChatDto chatDto) {
        if (chatDto.getContent() == null) {
            chatDto.setContent("");
        }
        if (ChatTypeEnum.getEnum(chatDto.getType()) == null) {
            chatDto.setType(ChatTypeEnum.TEXT.name());
        }
        chatDto.setId(IdWorker.getId());
        String conversationId = ChatUtil.MathConversationId(chatDto.getSenderId(), chatDto.getReceiverId());
        chatDto.setConversationId(conversationId);
        chatDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
        chatDto.setStatus(ChatStatusEnum.SENDING.getStatus());
        kafkaTemplate.send(topic, chatDto);
    }

    @Override
    public void add(ChatDto chatDto) {
        Chat chat = convertor.dtoToPojo(chatDto);
        chat.setStatus(ChatStatusEnum.UNREAD.getStatus());
        messageMapper.insert(chat);
    }

    @Override
    public void addBatch(Collection<ChatDto> chatDtos) {
        List<Chat> list = chatDtos.stream().map(chatDto -> {
            Chat chat = convertor.dtoToPojo(chatDto);
            chat.setStatus(ChatStatusEnum.UNREAD.getStatus());
            return chat;
        }).toList();
        messageMapper.insertBatch(list);
    }


    @Override
    public void deleteById(Long messageId) {
        messageMapper.deleteById(messageId);
    }


    @Override
    public Map<String, List<ChatDto>> getChatByConversation(Collection<String> conversations, Integer page) {
        Page<Chat> searchPage = new Page<>(page, pageSize);
        searchPage.setSearchCount(false);
        Map<String, List<ChatDto>> messageDtos = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(conversations.size());
        conversations.forEach(conversation -> {
            Thread.startVirtualThread(() -> {
                List<ChatDto> messages = this.getChatHistory(conversation, page, pageSize);
                messageDtos.put(conversation, messages);
                latch.countDown();
            });
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new ChirpException(Code.ERR_SYSTEM, "服务器繁忙");
        }
        return messageDtos;
    }

    @Override
    public List<ChatDto> getChatHistory(Long receiverId, Long senderId, Integer page) {
        String conversationId = ChatUtil.MathConversationId(senderId, receiverId);
        return getChatHistory(conversationId, page, pageSize);
    }

    @Override
    public List<ChatDto> getChatHistory(String conversationId, Integer page, Integer size) {
        Page<Chat> searchPage = new Page<>(page, size);
        searchPage.setSearchCount(false);
        return messageMapper.selectPage(searchPage, new LambdaQueryWrapper<Chat>()
                        .eq(Chat::getConversationId, conversationId)
                        .in(Chat::getStatus, ChatStatusEnum.READ.getStatus(), ChatStatusEnum.UNREAD.getStatus())
                        .orderByDesc(Chat::getCreateTime))
                .getRecords()
                .stream()
                .map(chat ->
                        convertor.pojoToDto(chat)
                )
                .toList();
    }

    @Override
    public Map<String, Integer> getUnreadCount(Collection<String> conversation, Long receiverId) {
        CountDownLatch latch = new CountDownLatch(conversation.size());
        Map<String, Integer> countMap = new HashMap<>();
        conversation.forEach(con -> {
            Thread.ofVirtual().start(() -> {
                countMap.put(con, messageMapper.selectCount(new LambdaQueryWrapper<Chat>()
                        .eq(Chat::getConversationId, con)
                        .eq(Chat::getReceiverId, receiverId)
                        .eq(Chat::getStatus, ChatStatusEnum.UNREAD.getStatus())).intValue());
                latch.countDown();
            });
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new ChirpException(Code.ERR_SYSTEM, "查询未读失败");
        }
        return countMap;

    }

    @Override
    public void markAsRead(Collection<String> conversationIds, Long receiverId) {
        messageMapper.update(null, new LambdaUpdateWrapper<Chat>()
                .set(Chat::getStatus, ChatStatusEnum.READ.getStatus())
                .eq(Chat::getStatus, ChatStatusEnum.UNREAD.getStatus())
                .in(Chat::getConversationId, conversationIds)
                .eq(Chat::getReceiverId, receiverId));
    }

    @Override
    public Set<String> getConversationByUserId(Long userId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Chat>()
                .eq(Chat::getReceiverId, userId)
                .or()
                .eq(Chat::getSenderId, userId)
                .in(Chat::getStatus, ChatStatusEnum.UNREAD.getStatus(), ChatStatusEnum.READ.getStatus())

                .select(Chat::getConversationId)).stream().map(Chat::getConversationId).collect(Collectors.toSet());
    }


}
