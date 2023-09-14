package com.zyq.chirp.gateway.service.impl;

import com.zyq.chirp.gateway.domain.pojo.Role;
import com.zyq.chirp.gateway.mapper.RoleMapper;
import com.zyq.chirp.gateway.service.RoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {
    @Resource
    RoleMapper roleMapper;

    @Override
    public List<Role> getRoleByUserId(Long userId) {
        return roleMapper.getByUserId(userId);
    }

    @Override
    public List<Role> getAllRole() {
        return roleMapper.selectList(null);
    }

    @Override
    @Transactional
    public Role addRole(Role role, List<Integer> permissionIds) {
        roleMapper.insert(role);
        permissionIds.forEach(id -> addPermission(id, role.getId()));
        return role;
    }

    @Override
    public void addPermission(Integer permissionId, Integer roleId) {
        roleMapper.addPermissionByRoleId(permissionId, roleId);
    }

    @Override
    public void addRoleToUser(Integer roleId, Long userId) {
        roleMapper.addToUser(roleId, userId);
    }

    @Override
    public void removeRoleFromUser(Integer roleId, Long userId) {
        roleMapper.removeFromUser(roleId, userId);
    }

    @Override
    public void removePermissionOfRole(Integer roleId, Integer permissionId) {
        roleMapper.removePermission(roleId, permissionId);
    }
}
