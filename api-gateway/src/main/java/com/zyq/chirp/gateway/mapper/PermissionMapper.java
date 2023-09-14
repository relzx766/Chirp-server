package com.zyq.chirp.gateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.gateway.domain.pojo.Permission;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionMapper extends BaseMapper<Permission> {
    @Select("""
            select tp.id, path, description
            from role_permission rp join tb_permission tp on rp.permission_id=tp.id
            where rp.role_id=#{roleId} 
            """)
    List<Permission> getByRoleId(Integer roleId);
}
