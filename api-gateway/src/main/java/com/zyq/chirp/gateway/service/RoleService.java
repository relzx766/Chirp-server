package com.zyq.chirp.gateway.service;


import com.zyq.chirp.gateway.domain.pojo.Role;

import java.util.List;


public interface RoleService {
    List<Role> getRoleByUserId(Long userId);

    List<Role> getAllRole();

    Role addRole(Role role, List<Integer> permissionIds);

    void addPermission(Integer permissionId, Integer roleId);

    void addRoleToUser(Integer roleId, Long userId);

    void removeRoleFromUser(Integer roleId, Long userId);

    void removePermissionOfRole(Integer roleId, Integer permissionId);
}
