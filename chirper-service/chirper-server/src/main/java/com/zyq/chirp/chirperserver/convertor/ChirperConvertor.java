package com.zyq.chirp.chirperserver.convertor;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;
import com.zyq.chirp.mediaclient.dto.MediaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChirperConvertor {
    @Mapping(target = "mediaKeys", source = "mediaKeys", qualifiedByName = "mediaId")
    Chirper dtoToPojo(ChirperDto chirperDto);

    @Mapping(target = "mediaKeys", source = "mediaKeys", qualifiedByName = "mediaConvertor")

    ChirperDto pojoToDto(Chirper chirper);

    @Named("mediaId")
    default List<Integer> getMediaId(List<MediaDto> mediaDtos) {
        if (mediaDtos == null) {
            return null;
        } else if (mediaDtos.isEmpty()) {
            return null;
        } else {
            return mediaDtos.stream().map(MediaDto::getId).toList();
        }
    }

    @Named("mediaConvertor")
    default List<MediaDto> toMedia(List<Integer> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().map(id -> {
            MediaDto mediaDto = new MediaDto();
            mediaDto.setId(id);
            return mediaDto;
        }).toList();
    }
}
