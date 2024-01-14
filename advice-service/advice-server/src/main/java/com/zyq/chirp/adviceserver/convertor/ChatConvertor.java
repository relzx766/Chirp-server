package com.zyq.chirp.adviceserver.convertor;

import com.zyq.chirp.adviceclient.dto.ChatDto;
import com.zyq.chirp.adviceclient.dto.ChatSettingDto;
import com.zyq.chirp.adviceserver.domain.pojo.Chat;
import com.zyq.chirp.adviceserver.domain.pojo.ChatSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatConvertor {
    @Mapping(source = "referenceId", target = "reference.id")

    ChatDto pojoToDto(Chat chat);

    @Mapping(target = "referenceId", source = "reference.id")
    Chat dtoToPojo(ChatDto chatDto);

    @Mapping(target = "chatReady", ignore = true)
    ChatSettingDto settingPojoToDto(ChatSetting chatSetting);

    ChatSetting settingDtoToPojo(ChatSetting chatSetting);
}
