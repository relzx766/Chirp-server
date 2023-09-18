package com.zyq.chirp.adviceserver.convertor;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")

public interface MessageConvertor {


    SiteMessageDto pojoToDto(Notification message);

    Notification dtoToPojo(SiteMessageDto messageDto);
}
