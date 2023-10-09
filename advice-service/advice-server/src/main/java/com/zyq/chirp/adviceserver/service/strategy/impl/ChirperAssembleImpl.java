package com.zyq.chirp.adviceserver.service.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.strategy.MessageAssembleStrategy;
import com.zyq.chirp.chirpclient.client.ChirperClient;
import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChirperAssembleImpl implements MessageAssembleStrategy {
    @Resource
    ObjectMapper objectMapper;
    @Resource
    ChirperClient chirperClient;
    @Resource
    UserClient userClient;

    @Override
    public List<SiteMessageDto> assemble(List<SiteMessageDto> messageDtos) {
        try {
            List<Long> chirperIds = new ArrayList<>();
            List<Long> senderIds = new ArrayList<>();
            messageDtos.forEach(messageDto -> {
                Long sonEntityId = messageDto.getSonEntity() != null ? Long.parseLong(messageDto.getSonEntity()) : null;
                Long entityId = messageDto.getEntity() != null ? Long.parseLong(messageDto.getEntity()) : null;
                chirperIds.add(sonEntityId);
                chirperIds.add(entityId);
                senderIds.add(messageDto.getSenderId());
            });
            CompletableFuture<List<ChirperDto>> chirperFuture = CompletableFuture.supplyAsync(() ->
                    chirperClient.getContent(chirperIds).getBody()
            ).exceptionally(throwable -> {
                throwable.printStackTrace();
                return List.of();
            });
            CompletableFuture<List<UserDto>> userFuture = CompletableFuture.supplyAsync(()
                    -> userClient.getShort(senderIds).getBody()
            ).exceptionally(throwable -> {
                throwable.printStackTrace();
                return List.of();
            });
            CompletableFuture<Void> allOf = CompletableFuture.allOf(chirperFuture, userFuture);
            allOf.join();
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
}
