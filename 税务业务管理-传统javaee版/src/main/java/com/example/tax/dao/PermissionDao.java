package com.example.tax.dao;

import com.example.tax.model.Permission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PermissionDao {

    public List<Permission> listAll(Connection connection) throws SQLException {
        return Jdbc.query(connection,
                "SELECT id, permission_code, permission_name, group_name, permission_type, "
                        + "parent_id, url_pattern, http_method, sort_order, status "
                        + "FROM sys_permission ORDER BY sort_order, id", this::map);
    }

    public List<String> listCodesByUserId(Connection connection, long userId) throws SQLException {
        return Jdbc.query(connection,
                "SELECT DISTINCT p.permission_code FROM sys_permission p "
                        + "JOIN sys_role_permission rp ON rp.permission_id = p.id "
                        + "JOIN sys_role r ON r.id = rp.role_id AND r.status = 'ACTIVE' "
                        + "JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = ? AND p.status = 'ACTIVE' "
                        + "ORDER BY p.permission_code",
                rs -> rs.getString(1), userId);
    }

    public List<String> listRoleCodesByUserId(Connection connection, long userId)
            throws SQLException {
        return Jdbc.query(connection,
                "SELECT r.role_code FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = ? AND r.status = 'ACTIVE' ORDER BY r.role_code",
                rs -> rs.getString(1), userId);
    }

    private Permission map(ResultSet rs) throws SQLException {
        Permission permission = new Permission();
        permission.setId(rs.getLong("id"));
        permission.setPermissionCode(rs.getString("permission_code"));
        permission.setPermissionName(rs.getString("permission_name"));
        permission.setGroupName(rs.getString("group_name"));
        permission.setPermissionType(rs.getString("permission_type"));
        long parentId = rs.getLong("parent_id");
        permission.setParentId(rs.wasNull() ? null : parentId);
        permission.setUrlPattern(rs.getString("url_pattern"));
        permission.setHttpMethod(rs.getString("http_method"));
        permission.setSortOrder(rs.getInt("sort_order"));
        permission.setStatus(rs.getString("status"));
        return permission;
    }
}
