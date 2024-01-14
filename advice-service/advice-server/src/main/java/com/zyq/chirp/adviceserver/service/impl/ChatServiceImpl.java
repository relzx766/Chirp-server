package com.zyq.chirp.adviceserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.dto.ChatSettingDto;
import com.zyq.chirp.adviceserver.convertor.ChatConvertor;
import com.zyq.chirp.adviceserver.domain.enums.CacheKey;
import com.zyq.chirp.adviceserver.domain.enums.ChatAllowEnum;
import com.zyq.chirp.adviceserver.domain.enums.ChatStatusEnum;
import com.zyq.chirp.adviceserver.domain.enums.ChatTypeEnum;
import com.zyq.chirp.adviceserver.domain.pojo.Chat;
import com.zyq.chirp.adviceserver.mapper.ChatMapper;
import com.zyq.chirp.adviceserver.mq.listener.RedisSubscribeListener;
import com.zyq.chirp.adviceserver.service.ChatService;
import com.zyq.chirp.adviceserver.service.ChatSettingService;
import com.zyq.chirp.adviceserver.util.ChatUtil;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.RelationDto;
import jakarta.annotation.Resource;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    @Resource
    ChatMapper messageMapper;
    @Resource
    ChatConvertor convertor;
    private static final Map<String, RedisSubscribeListener> redisSubMap = new HashMap<>();
    @Value("${default-config.page-size}")
    Integer pageSize;
    @Value("${mq.topic.site-message.chat}")
    String topic;
    @Resource
    RedisTemplate redisTemplate;
    @Value("${default-config.conversation-cache-size}")
    Integer conversationCacheSize;
    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;
    @Resource
    RedisMessageListenerContainer redisMessageListenerContainer;
    @Resource
    ChatSettingService chatSettingService;
    @Resource
    UserClient userClient;
    @Value("${mq.topic.socket-connect}")
    private String connectTopic;
    @Value("${mq.topic.site-message.user}")
    private String messageTopic;
    @Value("${mq.topic.socket-disconnect}")

    private String disconnectTopic;

    @Override
    public void send(ChatDto chatDto) {

        try {
            if (chatDto.getContent() == null) {
                chatDto.setContent("");
            }
            if (ChatTypeEnum.getEnum(chatDto.getType()) == null) {
                chatDto.setType(ChatTypeEnum.TEXT.name());
            }
            //为防止用户篡改信息，仅发送引用消息的id，同时保存引用消息
            ChatDto referCopy = chatDto.getReference();
            Optional.ofNullable(referCopy).ifPresent(reference -> {
                ChatDto refer = ChatDto.builder().id(reference.getId()).build();
                chatDto.setReference(refer);
            });
            chatDto.setId(IdWorker.getId());
            String conversationId = ChatUtil.mathConversationId(chatDto.getSenderId(), chatDto.getReceiverId());
            chatDto.setConversationId(conversationId);
            chatDto.setCreateTime(new Timestamp(System.currentTimeMillis()));
            if (this.canSendChat(chatDto.getSenderId(), chatDto.getReceiverId())) {
                chatDto.setStatus(ChatStatusEnum.SENDING.name());
                kafkaTemplate.send(topic, chatDto);
                chatDto.setStatus(ChatStatusEnum.UNREAD.name());
            } else {
                chatDto.setStatus(ChatStatusEnum.FAILED.name());
                chatDto.setFailedMsg("对方拒绝接收你的私信");
            }

            //将引用消息写回
            chatDto.setReference(referCopy);
        } catch (Exception e) {
            chatDto.setStatus(ChatStatusEnum.FAILED.name());
            chatDto.setFailedMsg("服务器错误，发送失败");
            log.error("发送消息失败", e);
        }
    }

    @Override
    public Boolean canSendChat(Long sender, Long receiver) {
        boolean check;
        if (sender.equals(receiver)) {
            check = true;
        } else {
            ValueOperations<String, Boolean> operations = redisTemplate.opsForValue();
            String key = STR."\{CacheKey.CAN_SEND_CHAT_CHECK}:\{sender}:\{receiver}";
            Boolean cacheCheck = operations.get(key);
            if (cacheCheck != null) {
                check = cacheCheck;
            } else {
                int rpcCount = 2;
                CountDownLatch latch = new CountDownLatch(rpcCount);
                var ref = new Object() {
                    ChatSettingDto settingDto;
                    RelationDto relationDto;
                };
                Thread.ofVirtual().start(() -> {
                    ResponseEntity<List<RelationDto>> relationRes = userClient.getRelationById(Set.of(receiver), sender);
                    if (relationRes.getStatusCode().is2xxSuccessful()) {
                        List<RelationDto> relationDtos = relationRes.getBody();
                        if (relationDtos != null && !relationDtos.isEmpty()) {
                            ref.relationDto = relationDtos.getFirst();
                        } else {
                            ref.relationDto = RelationDto.unFollow(receiver, sender);
                        }
                    }
                    latch.countDown();
                });
                Thread.ofVirtual().start(() -> {
                    ref.settingDto = chatSettingService.getByUserId(receiver);
                    latch.countDown();
                });
                try {
                    latch.await();
                    check = ((ChatAllowEnum.EVERYONE.ordinal() == ref.settingDto.getAllow() && !ref.relationDto.getIsBlock())
                            || ref.relationDto.getIsFollow()) && ref.settingDto.getChatReady();
                    Duration expire = Duration.ofSeconds(1);
                    operations.set(key, check, expire);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }

        }
        return check;
    }

    @Override
    public void add(ChatDto chatDto) {
        Chat chat = convertor.dtoToPojo(chatDto);
        ChatStatusEnum status = Objects.equals(chat.getSenderId(), chat.getReceiverId()) ? ChatStatusEnum.READ : ChatStatusEnum.UNREAD;
        chat.setStatus(status.name());
        messageMapper.insert(chat);
    }

    @Override
    public void connect(Long userId, Session session) {
        kafkaTemplate.send(connectTopic, userId);
        if (redisSubMap.containsKey(userId.toString())) {
            redisSubMap.get(userId.toString()).addSession(session);
        } else {
            RedisSubscribeListener subscribeListener = new RedisSubscribeListener(session);
            redisSubMap.put(userId.toString(), subscribeListener);
            redisMessageListenerContainer.addMessageListener(subscribeListener, new ChannelTopic(messageTopic + userId));
        }
    }

    @Override
    public void disconnect(Long userId, Session session) {
        kafkaTemplate.send(disconnectTopic, userId);
        RedisSubscribeListener listener = redisSubMap.get(userId.toString());
        listener.removeSession(session.getId());
        if (listener.getOpenSessionSize() <= 0) {
            redisMessageListenerContainer.removeMessageListener(listener);
            redisSubMap.remove(userId.toString());
        }
    }

    @Override
    public void addBatch(Collection<ChatDto> chatDtos) {
        List<Chat> list = chatDtos.stream().map(chatDto -> {
            Chat chat = convertor.dtoToPojo(chatDto);
            if (chat.getSenderId().equals(chat.getReceiverId())) {
                chat.setStatus(ChatStatusEnum.READ.name());
            } else {
                chat.setStatus(ChatStatusEnum.UNREAD.name());
            }
            return chat;
        }).toList();
        messageMapper.insertBatch(list);
        Thread.ofVirtual().start(() -> this.cacheChatByScore(chatDtos));
    }

    @Override
    public void cacheChatByScore(Collection<ChatDto> chatList) {
        ZSetOperations<String, Long> opsForZSet = redisTemplate.opsForZSet();
        Map<String, Set<ZSetOperations.TypedTuple<Long>>> scoreMap = new HashMap<>();
        chatList.forEach(chat -> {
            String conversationId = ChatUtil.mathConversationId(chat.getSenderId(), chat.getReceiverId());
            ZSetOperations.TypedTuple<Long> typedTuple = ZSetOperations.TypedTuple.of(chat.getId(), (double) chat.getCreateTime().getTime());
            if (scoreMap.containsKey(conversationId)) {
                scoreMap.get(conversationId).add(typedTuple);
            } else {
                scoreMap.put(conversationId, new HashSet<>(Set.of(typedTuple)));
            }
        });
        scoreMap.forEach((conversationId, tuples) -> {
            String key = STR. "\{ CacheKey.CONVERSATION_KEY.getKey() }:\{ conversationId }" ;
            opsForZSet.add(key, tuples);
            //保留最新的10条消息
            opsForZSet.removeRange(key, 0, -conversationCacheSize - 1);
        });
    }

    @Override
    public List<Long> getConvTop(Collection<String> conversations) {
        ZSetOperations<String, Long> opsForZSet = redisTemplate.opsForZSet();
        List<Long> result = new ArrayList<>();
        conversations.forEach(conv -> {
            Set<Long> range = opsForZSet.range(STR. "\{ CacheKey.CONVERSATION_KEY.getKey() }:\{ conv }" , 0, -1);
            if (range != null) {
                result.addAll(range);
            }
        });
        return result;
    }

    @Override
    public List<ChatDto> getChatIndex(Long userId) {
        Set<String> conversations = this.getConversationByUserId(userId);
        List<Long> messageIds = this.getConvTop(conversations);
        ArrayList<ChatDto> messages = new ArrayList<>();
        this.getById(messageIds).forEach(chatDto -> {
            messages.add(chatDto);
            conversations.remove(chatDto.getConversationId());
        });
        Collection<List<ChatDto>> values = this.getChatByConversation(conversations, userId, 1).values();
        values.forEach(messages::addAll);
        return messages;
    }


    @Override
    public void deleteById(Long messageId) {
        messageMapper.deleteById(messageId);
    }

    @Override
    public void markAsDel(Long messageId, Long userId) {
        boolean update = messageMapper.update(null, new LambdaUpdateWrapper<Chat>()
                .set(Chat::getStatus, userId)
                .eq(Chat::getId, messageId)
                .and(true, chatLambdaUpdateWrapper ->
                        chatLambdaUpdateWrapper.eq(Chat::getSenderId, userId)
                                .or()
                                .eq(Chat::getReceiverId, userId))
                .in(Chat::getStatus, ChatStatusEnum.READ.name(), ChatStatusEnum.UNREAD.name())) > 0;
        //更新失败则代表对方已删除
        if (!update) {
            messageMapper.update(null, new LambdaUpdateWrapper<Chat>()
                    .set(Chat::getStatus, ChatStatusEnum.DELETE.name())
                    .and(true, chatLambdaUpdateWrapper ->
                            chatLambdaUpdateWrapper.eq(Chat::getSenderId, userId)
                                    .or()
                                    .eq(Chat::getReceiverId, userId))
                    .eq(Chat::getId, messageId));
        }
    }

    @Override
    public void markAsDel(String conversationId, Long userId) {
        if (ChatUtil.isSelfTaking(conversationId)) {
            messageMapper.update(null, new LambdaUpdateWrapper<Chat>()
                    .set(Chat::getStatus, ChatStatusEnum.DELETE.name())
                    .in(Chat::getStatus, ChatStatusEnum.READ.name(), ChatStatusEnum.UNREAD.name(), userId.toString())
                    .eq(Chat::getConversationId, conversationId));
        } else {
            String[] member = ChatUtil.splitConversation(conversationId);
            boolean isMember = false;
            for (String uId : member) {
                if (uId.equals(userId.toString())) {
                    isMember = true;
                    break;
                }
            }
            if (isMember) {
                Map<String, List<Chat>> collect = messageMapper.selectList(new LambdaQueryWrapper<Chat>()
                                .select(Chat::getId, Chat::getStatus)
                                .eq(Chat::getConversationId, conversationId))
                        .stream()
                        .filter(chat -> !chat.getStatus().equals(ChatStatusEnum.DELETE.name()) || !chat.getStatus().equals(userId.toString()))
                        .collect(Collectors.groupingBy(Chat::getStatus));
                collect.forEach((status, chatList) -> {
                    if (ChatStatusEnum.READ.name().equals(status) || ChatStatusEnum.UNREAD.name().equals(status)) {
                        messageMapper.updateStatusBatch(chatList, userId.toString());
                    } else {
                        messageMapper.updateStatusBatch(chatList, ChatStatusEnum.DELETE.name());
                    }
                });
            }
        }
    }

    @Override
    public List<ChatDto> getById(Collection<Long> messageIds) {
        if (messageIds != null && !messageIds.isEmpty()) {
            return messageMapper.selectList(new LambdaQueryWrapper<Chat>()
                            .in(Chat::getId, messageIds)
                            .notIn(Chat::getStatus, ChatStatusEnum.DELETE, StpUtil.getLoginIdAsLong())
                            .orderByDesc(Chat::getCreateTime))
                    .stream()
                    .map(chat -> convertor.pojoToDto(chat))
                    .toList();
        }
        return List.of();
    }


    @Override
    public Map<String, List<ChatDto>> getChatByConversation(Collection<String> conversations, Long userId, Integer page) {
        Page<Chat> searchPage = new Page<>(page, pageSize);
        searchPage.setSearchCount(false);
        Map<String, List<ChatDto>> messageDtos = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(conversations.size());
        conversations.forEach(conversation -> {
            Thread.startVirtualThread(() -> {
                List<ChatDto> messages = this.getChatHistory(conversation, userId, page, pageSize);
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
    public List<ChatDto> getChatHistory(Long currentUserId, Long otherUserId, Integer page) {
        String conversationId = ChatUtil.mathConversationId(otherUserId, currentUserId);
        return getChatHistory(conversationId, currentUserId, page, pageSize);
    }

    @Override
    public List<ChatDto> getChatHistory(String conversationId, Long userId, Integer page, Integer size) {
        Page<Chat> searchPage = new Page<>(page, size);
        searchPage.setSearchCount(false);
        List<ChatDto> chatDtos = messageMapper.selectPage(searchPage, new LambdaQueryWrapper<Chat>()
                        .eq(Chat::getConversationId, conversationId)
                        .notIn(Chat::getStatus, ChatStatusEnum.DELETE, userId)
                        .orderByDesc(Chat::getCreateTime))
                .getRecords()
                .stream()
                .map(chat ->
                        convertor.pojoToDto(chat)
                )
                .toList();
        return this.getReference(chatDtos);
    }

    @Override
    public List<ChatDto> getReference(List<ChatDto> chatDtos) {
        Set<Long> referenceIds = new HashSet<>();
        chatDtos.stream().map(ChatDto::getReference).forEach(reference -> {
            if (reference != null && reference.getId() != null) {
                referenceIds.add(reference.getId());
            }
        });
        if (!referenceIds.isEmpty()) {
            Map<Long, Chat> referMap = messageMapper.selectList(new LambdaQueryWrapper<Chat>()
                            .in(Chat::getId, referenceIds)
                            .notIn(Chat::getStatus, ChatStatusEnum.DELETE.name()))
                    .stream().collect(Collectors.toMap(Chat::getId, Function.identity()));
            chatDtos.forEach(chatDto -> {
                ChatDto reference = chatDto.getReference();
                if (reference != null && reference.getId() != null) {
                    Chat chat = referMap.get(reference.getId());
                    Optional.ofNullable(chat)
                            .ifPresent(present -> {
                                chatDto.setReference(convertor.pojoToDto(present));
                            });
                }
            });
        }
        return chatDtos;
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
                        .eq(Chat::getStatus, ChatStatusEnum.UNREAD)).intValue());
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
                .set(Chat::getStatus, ChatStatusEnum.READ)
                .eq(Chat::getStatus, ChatStatusEnum.UNREAD)
                .in(Chat::getConversationId, conversationIds)
                .eq(Chat::getReceiverId, receiverId));
    }

    @Override
    public Set<String> getConversationByUserId(Long userId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Chat>()
                .notIn(Chat::getStatus, ChatStatusEnum.DELETE, userId)
                .and(true, chatLambdaQueryWrapper -> chatLambdaQueryWrapper.eq(Chat::getReceiverId, userId)
                        .or()
                        .eq(Chat::getSenderId, userId))
                .select(Chat::getConversationId)).stream().map(Chat::getConversationId).collect(Collectors.toSet());
    }


}
