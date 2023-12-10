package com.zyq.chirp.adviceserver.strategy.impl;

import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.service.ChatService;
import com.zyq.chirp.adviceserver.strategy.MessageAssembleStrategy;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component
@Slf4j
public class ChatAssembleStrategy implements MessageAssembleStrategy<ChatDto> {
    @Resource
    DefaultMessageAssembleStrategy<ChatDto> defaultMessageAssembleStrategy;
    @Resource
    ChatService chatService;

    @Override
    public List<ChatDto> assemble(List<ChatDto> messages) {
        CountDownLatch latch = new CountDownLatch(2);
        Thread.ofVirtual().start(() -> {
            defaultMessageAssembleStrategy.assemble(messages);
            latch.countDown();
        });
        Thread.ofVirtual().start(() -> {
            chatService.getReference(messages);
            List<ChatDto> reference = messages.stream().map(ChatDto::getReference).filter(chatDtoReference -> chatDtoReference != null && chatDtoReference.getSenderId() != null).toList();
            defaultMessageAssembleStrategy.assemble(reference);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new ChirpException(Code.ERR_SYSTEM, "线程终止，服务器繁忙");
        }
        return messages;
    }
}
