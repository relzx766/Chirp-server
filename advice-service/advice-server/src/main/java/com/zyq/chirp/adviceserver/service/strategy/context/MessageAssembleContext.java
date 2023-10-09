package com.zyq.chirp.adviceserver.service.strategy.context;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.service.strategy.MessageAssembleStrategy;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class MessageAssembleContext {
    private MessageAssembleStrategy strategy;

    public List<SiteMessageDto> assemble(List<SiteMessageDto> messageDtos) {
        return strategy.assemble(messageDtos);
    }
}
