package com.example.tax.dao;

import com.example.tax.dto.UserFilter;
import com.example.tax.model.Role;
import com.example.tax.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    private static final String BASE_COLUMNS =
            "u.id, u.username, u.password_hash, u.display_name, u.status, "
                    + "u.last_login_at, u.created_at, u.updated_at, u.deleted";

    public Optional<User> findByUsername(Connection connection, String username)
            throws SQLException {
        User user = Jdbc.queryOne(connection,
                "SELECT " + BASE_COLUMNS + " FROM sys_user u WHERE u.username = ? AND u.deleted = 0",
                this::map, username);
        if (user != null) {
            user.setRoles(new RoleDao().listByUserId(connection, user.getId()));
        }
        return Optional.ofNullable(user);
    }

    public Optional<User> findById(Connection connection, long id) throws SQLException {
        User user = Jdbc.queryOne(connection,
                "SELECT " + BASE_COLUMNS + " FROM sys_user u WHERE u.id = ? AND u.deleted = 0",
                this::map, id);
        if (user != null) {
            user.setRoles(new RoleDao().listByUserId(connection, user.getId()));
        }
        return Optional.ofNullable(user);
    }

    public List<User> search(Connection connection, UserFilter filter, int offset, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT " + BASE_COLUMNS
                + " FROM sys_user u"
                + " LEFT JOIN sys_user_role ur ON ur.user_id = u.id"
                + " WHERE u.deleted = 0");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        sql.append(" ORDER BY u.id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        List<User> users = Jdbc.query(connection, sql.toString(), this::map, params.toArray());
        RoleDao roleDao = new RoleDao();
        for (User user : users) {
            user.setRoles(roleDao.listByUserId(connection, user.getId()));
        }
        return users;
    }

    public long count(Connection connection, UserFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT u.id) FROM sys_user u"
                + " LEFT JOIN sys_user_role ur ON ur.user_id = u.id"
                + " WHERE u.deleted = 0");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        return Jdbc.queryCount(connection, sql.toString(), params.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> params, UserFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append(" AND (u.username LIKE ? OR u.display_name LIKE ?)");
            String like = "%" + filter.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (filter.getRoleId() != null) {
            sql.append(" AND ur.role_id = ?");
            params.add(filter.getRoleId());
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append(" AND u.status = ?");
            params.add(filter.getStatus());
        }
    }

    public long insert(Connection connection, User user) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO sys_user (username, password_hash, display_name, status, "
                        + "created_at, updated_at, deleted) VALUES (?, ?, ?, ?, "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                user.getUsername(), user.getPasswordHash(), user.getDisplayName(), user.getStatus());
    }

    public int updateProfile(Connection connection, long id, String displayName, String status)
            throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_user SET display_name = ?, status = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND deleted = 0",
                displayName, status, id);
    }

    public int updatePassword(Connection connection, long id, String passwordHash)
            throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_user SET password_hash = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND deleted = 0",
                passwordHash, id);
    }

    public int updateLastLogin(Connection connection, long id) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_user SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    }

    public int setStatus(Connection connection, long id, String status) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_user SET status = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND deleted = 0",
                status, id);
    }

    public int softDelete(Connection connection, long id) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE sys_user SET deleted = 1, status = 'DISABLED', "
                        + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0", id);
    }

    public void replaceRoles(Connection connection, long userId, List<Long> roleIds)
            throws SQLException {
        Jdbc.update(connection, "DELETE FROM sys_user_role WHERE user_id = ?", userId);
        for (Long roleId : roleIds) {
            Jdbc.update(connection, "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                    userId, roleId);
        }
    }

    public List<Role> listRoles(Connection connection, long userId) throws SQLException {
        return new RoleDao().listByUserId(connection, userId);
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));
        user.setStatus(rs.getString("status"));
        user.setLastLoginAt(rs.getTimestamp("last_login_at"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        user.setDeleted(rs.getBoolean("deleted"));
        return user;
    }
}
