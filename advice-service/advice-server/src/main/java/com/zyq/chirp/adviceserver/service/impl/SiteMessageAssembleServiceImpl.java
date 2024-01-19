package com.zyq.chirp.adviceserver.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceclient.enums.EntityType;
import com.zyq.chirp.adviceclient.enums.EventType;
import com.zyq.chirp.adviceserver.service.SiteMessageAssembleService;
import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SiteMessageAssembleServiceImpl implements SiteMessageAssembleService {
    @Resource
    ObjectMapper objectMapper;
    @Resource
    ChirperClient chirperClient;
    @Resource
    UserClient userClient;

    @Override
    public List<NotificationDto> assemble(List<NotificationDto> messageDtos) {
        return null;
    }

 /*   @Override
    public List<NotificationDto> assemble(List<NotificationDto> messageDtos) {
        List<NotificationDto> chirper = new ArrayList<>();
        List<NotificationDto> user = new ArrayList<>();
        List<NotificationDto> other = new ArrayList<>();
        messageDtos.forEach(siteMessageDto -> {
            if (EntityType.CHIRPER.name().equals(siteMessageDto.getEntityType())) {
                chirper.add(siteMessageDto);
            } else if (EntityType.USER.name().equals(siteMessageDto.getEntityType()) || EventType.CHAT.name().equals(siteMessageDto.getEvent())) {
                user.add(siteMessageDto);
            } else {
                other.add(siteMessageDto);
            }
        });
        if (!chirper.isEmpty()) {
            chirperAssemble(chirper);
        }
        if (!user.isEmpty()) {
            userAssemble(user);
        }
        other.addAll(chirper);
        other.addAll(user);
        return other;
    }

    public List<NotificationDto> chirperAssemble(List<NotificationDto> messageDtos) {
        try {
            List<Long> chirperIds = new ArrayList<>();
            List<Long> senderIds = new ArrayList<>();
            messageDtos.forEach(messageDto -> {
                try {
                    Long sonEntityId = messageDto.getSonEntity() != null ? Long.parseLong(messageDto.getSonEntity()) : null;
                    Long entityId = messageDto.getEntity() != null ? Long.parseLong(messageDto.getEntity()) : null;
                    chirperIds.add(sonEntityId);
                    chirperIds.add(entityId);
                    senderIds.add(messageDto.getSenderId());
                } catch (Exception e) {
                    log.warn("组装完整站内信，转化实体id时发生错误{}", e.getCause());
                }

            });
            CompletableFuture<List<ChirperDto>> chirperFuture = CompletableFuture.supplyAsync(() ->
                    chirperClient.getContent(chirperIds).getBody()
            ).exceptionally(throwable -> {
                throwable.printStackTrace();
                return List.of();
            });
            CompletableFuture<List<UserDto>> userFuture = CompletableFuture.supplyAsync(()
                    -> userClient.getBasicInfo(senderIds).getBody()
            ).exceptionally(throwable -> {
                throwable.printStackTrace();
                return List.of();
            });
            CompletableFuture.allOf(chirperFuture, userFuture).join();
            Map<Long, ChirperDto> chirperMap = chirperFuture.get()
                    .stream().collect(Collectors.toMap(ChirperDto::getId, Function.identity()));
            Map<Long, UserDto> userMap = userFuture.get()
                    .stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
            messageDtos.forEach(messageDto -> {
                try {
                    Long sonEntityId = messageDto.getSonEntity() != null ? Long.parseLong(messageDto.getSonEntity()) : null;
                    Long entityId = messageDto.getEntity() != null ? Long.parseLong(messageDto.getEntity()) : null;
                    messageDto.setEntity(objectMapper.writeValueAsString(chirperMap.get(entityId)));
                    messageDto.setSonEntity(objectMapper.writeValueAsString(chirperMap.get(sonEntityId)));
                    UserDto sender = userMap.get(messageDto.getSenderId());
                    messageDto.setSenderName(sender.getNickname());
                    messageDto.setSenderAvatar(sender.getSmallAvatarUrl());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return messageDtos;
    }

    public List<NotificationDto> userAssemble(List<NotificationDto> messageDtos) {
        try {
            List<Long> senderIds = messageDtos.stream().map(NotificationDto::getSenderId).toList();
            Map<Long, UserDto> senderMap = Objects.requireNonNull(userClient.getBasicInfo(senderIds).getBody())
                    .stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
            messageDtos.forEach(messageDto -> {
                UserDto sender = senderMap.get(messageDto.getSenderId());
                messageDto.setSenderName(sender.getNickname());
                messageDto.setSenderAvatar(sender.getSmallAvatarUrl());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messageDtos;
    }*/

}
