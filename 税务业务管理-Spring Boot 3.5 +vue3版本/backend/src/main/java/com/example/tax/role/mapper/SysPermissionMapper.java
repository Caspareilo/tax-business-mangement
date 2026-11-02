package com.example.tax.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tax.role.entity.SysPermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("""
            SELECT p.* FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
            ORDER BY p.sort_order, p.id
            """)
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT DISTINCT p.* FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId} AND p.status = 'ACTIVE'
            ORDER BY p.sort_order, p.id
            """)
    List<SysPermission> selectByUserId(@Param("userId") Long userId);
}
