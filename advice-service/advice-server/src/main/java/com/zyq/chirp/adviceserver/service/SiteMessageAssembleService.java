package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;

import java.util.List;

public interface SiteMessageAssembleService {
    List<SiteMessageDto> assemble(List<SiteMessageDto> messageDtos);
}
