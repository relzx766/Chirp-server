package com.zyq.chirp.gateway.service.impl;

import com.zyq.chirp.gateway.domain.pojo.Permission;
import com.zyq.chirp.gateway.mapper.PermissionMapper;
import com.zyq.chirp.gateway.service.PermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {
    @Resource
    PermissionMapper permissionMapper;

    @Override
    public Permission add(Permission permission) {
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    public List<Permission> add(List<Permission> permissions) {
        permissions.forEach(permission -> permissionMapper.insert(permission));
        return permissions;
    }

    @Override
    public boolean delete(Integer id) {
        return permissionMapper.deleteById(id) > 0;
    }

    @Override
    public boolean delete(List<Integer> ids) {
        return permissionMapper.deleteBatchIds(ids) == ids.size();
    }

    @Override
    public Permission update(Permission permission) {
        permissionMapper.updateById(permission);
        return permission;
    }

    @Override
    public List<Permission> getAll() {
        return permissionMapper.selectList(null);
    }

    @Override
    public List<Permission> getByRoleId(Integer roleId) {
        return permissionMapper.getByRoleId(roleId);
    }
}
