package com.zyq.chirp.mediaserver.convertor;

import com.zyq.chirp.mediaclient.dto.MediaDto;
import com.zyq.chirp.mediaserver.domain.pojo.Media;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")

public interface MediaConvertor {
    Media dtoToPojo(MediaDto mediaDto);

    MediaDto pojoToDto(Media media);
}
