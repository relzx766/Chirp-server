package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.NotificationDto;

import java.util.List;

public interface SiteMessageAssembleService {
    List<NotificationDto> assemble(List<NotificationDto> messageDtos);
}
