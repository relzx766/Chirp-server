package com.zyq.chirp.adviceserver.strategy.impl;

import com.zyq.chirp.adviceclient.dto.SiteMessage;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DefaultMessageAssembleStrategy<T extends SiteMessage> implements MessageAssembleStrategy<T> {

    @Resource
    UserClient userClient;

    @Override
    public List<T> assemble(List<T> messages) {
        Set<Long> userIds = new HashSet<>();
        messages.forEach(msg -> {
            userIds.add(msg.getSenderId());
            userIds.add(msg.getReceiverId());
        });
        Map<Long, UserDto> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            Map<Long, UserDto> collect = userClient.getBasicInfo(userIds).getBody().stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
            userMap.putAll(collect);
        }
        if (!userMap.isEmpty()) {
            messages.forEach(msg -> {
                UserDto sender = userMap.get(msg.getSenderId());
                UserDto receiver = userMap.get(msg.getReceiverId());
                if (sender != null) {
                    msg.setSenderName(sender.getNickname());
                    msg.setSenderAvatar(sender.getSmallAvatarUrl());
                }
                if (receiver != null) {
                    msg.setReceiverName(receiver.getNickname());
                    msg.setReceiverAvatar(receiver.getSmallAvatarUrl());
                }
            });
        }
        return messages;
    }
}
