package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;

import java.util.Collection;
import java.util.List;

public interface NotificationService {
    void save(Notification notification);

    void saveBatch(Collection<Notification> notifications);



    List<NotificationDto> getPageByReceiverId(Integer page, Long receiverId);

    Integer getUnReadCount(Long receiverId);

    void readAll(Long receiverId);

}
