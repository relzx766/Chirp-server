package com.zyq.chirp.gateway.domain.vo;

import com.zyq.chirp.gateway.domain.pojo.Permission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleVo {
    private Integer id;
    private String name;
    private String description;
    private List<Permission> permissions;
}
