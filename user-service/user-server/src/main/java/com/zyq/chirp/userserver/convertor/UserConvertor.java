package com.zyq.chirp.userserver.convertor;

import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userserver.model.pojo.User;
import com.zyq.chirp.userserver.model.vo.UserVo;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface UserConvertor {

    User dtoToPojo(UserDto userDto);

    UserDto pojoToDto(User user);

    UserDto voToDto(UserVo userVo);
}
