package com.zyq.chirp.gateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.gateway.domain.pojo.Role;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleMapper extends BaseMapper<Role> {
    @Select("""
            select tr.id, name, description
             from user_role ur
            join tb_role tr on ur.role_id = tr.id
            where ur.user_id=#{userId}
            """)
    List<Role> getByUserId(Long userId);

    @Insert("""
            insert into role_permission(role_id, permission_id)
            values (#{roleId} ,#{permissionId} )
            """)
    int addPermissionByRoleId(@Param("permissionId") Integer PermissionId,
                              @Param("roleId") Integer roleId);

    @Insert("""
            insert into user_role(user_id, role_id) VALUES (#{userId} ,#{roleId} )
            """)
    int addToUser(@Param("roleId") Integer roleId, @Param("userId") Long userId);

    @Delete("delete from user_role where role_id=#{roleId} and user_id=#{userId} ")
    int removeFromUser(@Param("roleId") Integer roleId, @Param("userId") Long userId);

    @Delete("""
            delete from role_permission where role_id=#{roleId} and permission_id=#{permissionId} 
            """)
    int removePermission(@Param("roleId") Integer roleId, @Param("permissionId") Integer permissionId);
}
