package com.zyq.chirp.userserver.service;


import com.zyq.chirp.userclient.dto.UserDto;

import java.util.Collection;
import java.util.List;

public interface UserService {
    UserDto save(UserDto userDto);

    boolean update(UserDto userDto);


    List<UserDto> getByIds(Collection<Long> userIds, Long currentUserId);

    UserDto getByUsername(String username, Long currentUserId);

    UserDto getDetailByEmail(String email);

    UserDto getDetailById(Long userId);

    UserDto getDetailByUsername(String username);

    boolean isUnExist(String username);

    boolean isEmailExist(String email);

    List<UserDto> search(String keyword, Long currentUserId, Integer page);

    List<UserDto> getBasicInfo(Collection<Long> userIds);

    /**
     * 获取用户的用户名及与我的关系
     *
     * @param userIds  用户
     * @param targetId 我
     * @return 用户的用户名及与我的关系
     */
    List<UserDto> getBasicInfo(Collection<Long> userIds, Long targetId);

    List<Long> getIdByUsername(Collection<String> username);

    void createUsernameBloom();

    boolean saveToUnBloom(String username);

    void createEmailBloom();

    boolean saveToEmailBloom(String email);


}
