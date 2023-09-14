package com.zyq.chirp.gateway.service;

import com.zyq.chirp.gateway.domain.pojo.Permission;

import java.util.List;

public interface PermissionService {
    Permission add(Permission permission);

    List<Permission> add(List<Permission> permissions);

    boolean delete(Integer id);

    boolean delete(List<Integer> ids);

    Permission update(Permission permission);

    List<Permission> getAll();

    List<Permission> getByRoleId(Integer roleId);
}
