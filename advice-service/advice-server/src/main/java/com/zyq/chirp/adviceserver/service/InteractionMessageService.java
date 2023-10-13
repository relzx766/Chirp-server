package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;

import java.util.Collection;
import java.util.List;

public interface InteractionMessageService {
    void save(Notification notification);

    void saveBatch(Collection<Notification> notifications);

    List<SiteMessageDto> getByReceiverId(Long receiverId);


    List<SiteMessageDto> getPageByReceiverId(Integer page, Long receiverId);

    Integer getUnReadCount(Long receiverId);

    void readAll(Long receiverId);
    List<SiteMessageDto> getUnReadByReceiverId(Long receiverId);

    void markAsRead(Long messageId, Long receiverId);

    void markAsRead(Collection<Long> messageIds, Long receiverId);


    /**
     * 将消息所需的一切信息组装，如实体信息
     *
     * @param messageDtos 简略消息
     * @return 详细消息
     */
    List<SiteMessageDto> combine(Collection<SiteMessageDto> messageDtos);

}
