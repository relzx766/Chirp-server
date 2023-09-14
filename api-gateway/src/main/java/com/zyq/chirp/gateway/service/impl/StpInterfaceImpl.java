package com.zyq.chirp.gateway.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.zyq.chirp.gateway.domain.pojo.Permission;
import com.zyq.chirp.gateway.domain.pojo.Role;
import com.zyq.chirp.gateway.service.PermissionService;
import com.zyq.chirp.gateway.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StpInterfaceImpl implements StpInterface {
    @Resource
    RoleService roleService;
    @Resource
    PermissionService permissionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {

        List<Role> roles = roleService.getRoleByUserId(Long.valueOf((String) loginId));
        List<Permission> permissions = new ArrayList<>();
        roles.forEach(role -> permissions.addAll(permissionService.getByRoleId(role.getId())));
        return permissions.stream()
                .map(Permission::getPath)
                .toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleService.getRoleByUserId(Long.valueOf((String) loginId)).stream()
                .map(Role::getName)
                .toList();
    }


}
