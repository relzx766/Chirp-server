package com.zyq.chirp.adviceserver.strategy.impl;

import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ChatAssembleStrategy implements MessageAssembleStrategy<ChatDto> {
    @Resource
    DefaultMessageAssembleStrategy<ChatDto> defaultMessageAssembleStrategy;

    @Override
    public List<ChatDto> assemble(List<ChatDto> messages) {
        return defaultMessageAssembleStrategy.assemble(messages);
    }
}
