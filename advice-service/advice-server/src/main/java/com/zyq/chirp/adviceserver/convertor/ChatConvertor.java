package com.zyq.chirp.adviceserver.convertor;

import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceserver.domain.pojo.Chat;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")

public interface ChatConvertor {
    ChatDto pojoToDto(Chat chat);

    Chat dtoToPojo(ChatDto chatDto);

}
