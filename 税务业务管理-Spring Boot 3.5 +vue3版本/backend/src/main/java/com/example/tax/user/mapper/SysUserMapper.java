package com.example.tax.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.tax.user.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT r.role_code FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 'ACTIVE'
            ORDER BY r.id
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.permission_code FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND p.status = 'ACTIVE' AND r.status = 'ACTIVE'
            """)
    List<String> selectPermissionCodes(@Param("userId") Long userId);

    @Select("""
            SELECT p.permission_code FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND p.status = 'ACTIVE' AND r.status = 'ACTIVE'
              AND p.permission_type = 'MENU'
            ORDER BY p.sort_order
            """)
    List<String> selectMenuCodes(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM tax_declaration WHERE created_by = #{userId} AND deleted = 0")
    long countDeclarationsByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM declaration_review WHERE reviewer_id = #{userId}")
    long countReviewsByUser(@Param("userId") Long userId);
}
