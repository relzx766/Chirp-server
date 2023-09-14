package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.domain.pojo.InteractionMessage;

import java.util.Collection;
import java.util.List;

public interface InteractionMessageService {
    void save(InteractionMessage interactionMessage);

    void saveBatch(Collection<InteractionMessage> interactionMessages);

    List<SiteMessageDto> getByReceiverId(Long receiverId);

    List<SiteMessageDto> getPageByReceiverId(Integer page, Long receiverId);

    List<SiteMessageDto> getUnReadByReceiverId(Long receiverId);

    void markAsRead(Long messageId, Long receiverId);

    void markAsRead(Collection<Long> messageIds, Long receiverId);

}
