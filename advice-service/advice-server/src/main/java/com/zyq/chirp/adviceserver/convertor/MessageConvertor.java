package com.zyq.chirp.adviceserver.convertor;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.domain.pojo.InteractionMessage;
import com.zyq.chirp.adviceserver.domain.vo.InteractionMessageVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")

public interface MessageConvertor {
    @Mapping(source = "chirperId", target = "targetId")
    SiteMessageDto voToDto(InteractionMessageVo messageVo);

    @Mapping(source = "chirperId", target = "targetId")
    SiteMessageDto pojoToDto(InteractionMessage message);

    @Mapping(target = "chirperId", source = "targetId")
    InteractionMessage dtoToPojo(SiteMessageDto messageDto);
}
