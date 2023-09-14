package com.zyq.chirp.chirperserver.convertor;

import com.zyq.chirp.chirpclient.dto.LikeDto;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LikeConvertor {

    LikeDto pojoToDto(Like like);

    Like dtoToPojo(LikeDto likeDto);
}
