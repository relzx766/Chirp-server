package com.zyq.chirp.userserver.service;


import com.zyq.chirp.userclient.dto.UserDto;

import java.util.Collection;
import java.util.List;

public interface UserService {
    UserDto save(UserDto userDto);

    boolean update(UserDto userDto);

    UserDto getById(Long userId, Long currentUserId);

    List<UserDto> getByIds(Collection<Long> userIds, Long currentUserId);

    UserDto getByUsername(String username, Long currentUserId);

    UserDto getDetailByEmail(String email);

    UserDto getDetailById(Long userId);

    UserDto getDetailByUsername(String username);

    boolean isExistByUsername(String username);

    boolean isExistByEmail(String email);

    List<UserDto> search(String keyword, Long currentUserId, Integer page);

    List<UserDto> getBasicInfo(Collection<Long> userIds);

    List<Long> getIdByUsername(Collection<String> username);

    void createUsernameBloom();

    boolean saveToUnBloom(String username);

    void createEmailBloom();

    boolean saveToEmailBloom(String email);


}
