package com.example.tax.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tax.role.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.* FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    long countUsers(@Param("roleId") Long roleId);
}
