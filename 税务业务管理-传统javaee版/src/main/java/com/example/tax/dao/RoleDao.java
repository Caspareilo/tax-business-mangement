package com.example.tax.dao;

import com.example.tax.model.Permission;
import com.example.tax.model.Role;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoleDao {

    public List<Role> listAll(Connection connection) throws SQLException {
        List<Role> roles = Jdbc.query(connection,
                "SELECT r.id, r.role_code, r.role_name, r.description, r.status, "
                        + "r.created_at, r.updated_at, "
                        + "(SELECT COUNT(*) FROM sys_user_role ur "
                        + " JOIN sys_user u ON u.id = ur.user_id AND u.deleted = 0 "
                        + " WHERE ur.role_id = r.id) AS user_count "
                        + "FROM sys_role r ORDER BY r.id",
                (ResultSet rs) -> {
                    Role role = map(rs);
                    role.setUserCount(rs.getLong("user_count"));
                    return role;
                });
        for (Role role : roles) {
            role.setPermissions(listPermissions(connection, role.getId()));
        }
        return roles;
    }

    public List<Role> listActive(Connection connection) throws SQLException {
        return Jdbc.query(connection,
                "SELECT id, role_code, role_name, description, status, created_at, updated_at, 0 "
                        + "FROM sys_role WHERE status = 'ACTIVE' ORDER BY id", this::map);
    }

    public List<Role> listByUserId(Connection connection, long userId) throws SQLException {
        return Jdbc.query(connection,
                "SELECT r.id, r.role_code, r.role_name, r.description, r.status, "
                        + "r.created_at, r.updated_at, 0 "
                        + "FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id "
                        + "WHERE ur.user_id = ? ORDER BY r.id",
                this::map, userId);
    }

    public Role findById(Connection connection, long id) throws SQLException {
        Role role = Jdbc.queryOne(connection,
                "SELECT id, role_code, role_name, description, status, created_at, updated_at, 0 "
                        + "FROM sys_role WHERE id = ?", this::map, id);
        if (role != null) {
            role.setPermissions(listPermissions(connection, id));
        }
        return role;
    }

    public Role findByCode(Connection connection, String roleCode) throws SQLException {
        return Jdbc.queryOne(connection,
                "SELECT id, role_code, role_name, description, status, created_at, updated_at, 0 "
                        + "FROM sys_role WHERE role_code = ?", this::map, roleCode);
    }

    public List<Permission> listPermissions(Connection connection, long roleId)
            throws SQLException {
        return Jdbc.query(connection,
                "SELECT p.id, p.permission_code, p.permission_name, p.group_name, "
                        + "p.permission_type, p.parent_id, p.url_pattern, p.http_method, "
                        + "p.sort_order, p.status "
                        + "FROM sys_permission p JOIN sys_role_permission rp ON rp.permission_id = p.id "
                        + "WHERE rp.role_id = ? ORDER BY p.sort_order, p.id",
                (ResultSet rs) -> {
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
                }, roleId);
    }

    public List<Long> listPermissionIds(Connection connection, long roleId) throws SQLException {
        return Jdbc.query(connection,
                "SELECT permission_id FROM sys_role_permission WHERE role_id = ? ORDER BY permission_id",
                rs -> rs.getLong(1), roleId);
    }

    public long insert(Connection connection, Role role) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO sys_role (role_code, role_name, description, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                role.getRoleCode(), role.getRoleName(), role.getDescription(), role.getStatus());
    }

    public int update(Connection connection, Role role) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_role SET role_name = ?, description = ?, status = ?, "
                        + "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                role.getRoleName(), role.getDescription(), role.getStatus(), role.getId());
    }

    public int setStatus(Connection connection, long id, String status) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_role SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status, id);
    }

    public void replacePermissions(Connection connection, long roleId, List<Long> permissionIds)
            throws SQLException {
        Jdbc.update(connection, "DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        for (Long permissionId : permissionIds) {
            Jdbc.update(connection,
                    "INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)",
                    roleId, permissionId);
        }
    }

    public long countUsers(Connection connection, long roleId) throws SQLException {
        return Jdbc.queryCount(connection,
                "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_user u ON u.id = ur.user_id "
                        + "AND u.deleted = 0 WHERE ur.role_id = ?", roleId);
    }

    private Role map(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getLong("id"));
        role.setRoleCode(rs.getString("role_code"));
        role.setRoleName(rs.getString("role_name"));
        role.setDescription(rs.getString("description"));
        role.setStatus(rs.getString("status"));
        role.setCreatedAt(rs.getTimestamp("created_at"));
        role.setUpdatedAt(rs.getTimestamp("updated_at"));
        return role;
    }
}
