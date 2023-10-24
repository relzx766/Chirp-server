package com.zyq.chirp.adviceserver.convertor;

import com.zyq.chirp.adviceclient.dto.NotificationDto;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")

public interface NoticeConvertor {


    NotificationDto pojoToDto(Notification message);

    Notification dtoToPojo(NotificationDto messageDto);
}
