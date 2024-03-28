package com.zyq.chirp.adviceserver.service;

import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    void saveBatch(Collection<Notification> notifications);


    List<NotificationDto> getSendable(Collection<NotificationDto> notificationDtos);

    List<NotificationDto> getPageByReceiverId(Integer page, Long receiverId);

    Integer getUnReadCount(Long receiverId);

    void readAll(Long receiverId);

}
