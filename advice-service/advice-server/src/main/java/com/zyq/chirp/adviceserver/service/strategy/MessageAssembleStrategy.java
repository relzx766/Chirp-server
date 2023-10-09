package com.zyq.chirp.adviceserver.service.strategy;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;

import java.util.List;

public interface MessageAssembleStrategy {
    List<SiteMessageDto> assemble(List<SiteMessageDto> messageDtos);
}
