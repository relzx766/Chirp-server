package com.zyq.chirp.communityserver.convertor;

import com.zyq.chirp.communityclient.dto.ApplyDto;
import com.zyq.chirp.communityserver.domain.pojo.Apply;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplyConvertor {
    Apply dtoToPojo(ApplyDto applyDto);

    ApplyDto pojoToDto(Apply apply);
}
