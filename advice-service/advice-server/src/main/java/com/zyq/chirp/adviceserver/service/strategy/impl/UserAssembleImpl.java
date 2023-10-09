package com.zyq.chirp.adviceserver.service.strategy.impl;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.strategy.MessageAssembleStrategy;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserAssembleImpl implements MessageAssembleStrategy {

    @Resource
    UserClient userClient;

    @Override
    public List<SiteMessageDto> assemble(List<SiteMessageDto> messageDtos) {
        try {
            List<Long> senderIds = messageDtos.stream().map(SiteMessageDto::getSenderId).toList();
            Map<Long, UserDto> senderMap = Objects.requireNonNull(userClient.getShort(senderIds).getBody())
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
    }
}
