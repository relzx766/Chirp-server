package com.zyq.chirp.authserver.service;

import com.zyq.chirp.authclient.dto.AuthDto;
import com.zyq.chirp.authclient.dto.PasswordResetDto;
import com.zyq.chirp.userclient.dto.UserDto;

import java.util.Collection;
import java.util.Map;

public interface AuthService {
    AuthDto login(AuthDto authDto);

    AuthDto signUp(UserDto userDto);

    AuthDto resetPwd(PasswordResetDto resetDto);
    boolean online(String id);

    boolean getIsOnline(String id);

    Map<String, Boolean> getIsOnline(Collection<String> id);

    boolean offline(String id);
}
