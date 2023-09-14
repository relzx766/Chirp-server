package com.zyq.chirp.chirperserver.convertor;

import com.zyq.chirp.chirpclient.dto.ChirperDto;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;
import com.zyq.chirp.chirperserver.domain.vo.ChirperVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChirperConvertor {
    Chirper dtoToPojo(ChirperDto chirperDto);

    ChirperDto pojoToDto(Chirper chirper);

    ChirperVo dtoToVo(ChirperDto chirperDto);

    ChirperDto voToDto(ChirperVo chirperVo);

    ChirperVo pojoToVo(Chirper chirper);
}
