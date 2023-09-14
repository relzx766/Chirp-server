package com.zyq.chirp.authserver.service;

import com.zyq.chirp.authclient.dto.RoleDto;

public interface RoleService {
    RoleDto getUserRole(Long userId);
}
