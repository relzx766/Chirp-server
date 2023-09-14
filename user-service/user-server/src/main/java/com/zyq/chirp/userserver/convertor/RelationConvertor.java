package com.zyq.chirp.userserver.convertor;

import com.zyq.chirp.userclient.dto.RelationDto;
import com.zyq.chirp.userserver.model.pojo.Relation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")

public interface RelationConvertor {
    RelationDto pojoToDto(Relation relation);

    Relation dtoToPojo(RelationDto relationDto);
}
