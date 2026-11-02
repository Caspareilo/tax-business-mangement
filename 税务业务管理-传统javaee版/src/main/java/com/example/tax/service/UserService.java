package com.example.tax.service;

import com.example.tax.dao.RoleDao;
import com.example.tax.dao.UserDao;
import com.example.tax.dto.Page;
import com.example.tax.dto.UserFilter;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.Role;
import com.example.tax.model.User;
import com.example.tax.security.PasswordUtil;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserService {

    private final UserDao userDao = new UserDao();
    private final RoleDao roleDao = new RoleDao();
    private final AuditService auditService = new AuditService();

    public Page<User> list(UserFilter filter, int page, int size) throws SQLException {
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = userDao.count(connection, filter);
            List<User> rows = userDao.search(connection, filter, (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public User findById(long id) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return userDao.findById(connection, id)
                    .orElseThrow(() -> new NotFoundException("用户不存在"));
        }
    }

    public List<Role> listRoles() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return roleDao.listActive(connection);
        }
    }

    public long create(SessionUser actor, String username, String displayName, String password,
                       List<Long> roleIds, String traceId) throws SQLException {
        requireManagePermission(actor);
        Map<String, String> errors = ValidationUtil.newErrors();
        ValidationUtil.requireUsername(errors, "username", "登录账号", username);
        ValidationUtil.requireText(errors, "displayName", "显示名称", displayName, 100);
        if (!PasswordUtil.isStrongEnough(password)) {
            errors.put("password", "初始密码至少 8 位，且必须同时包含字母和数字");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            errors.put("roleIds", "至少分配一个角色");
        }
        ValidationUtil.throwIfAny(errors, "请检查用户表单");

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (userDao.findByUsername(connection, username.trim()).isPresent()) {
                    throw new ValidationException("登录账号已存在",
                            Map.of("username", "该登录账号已被使用"));
                }
                User user = new User();
                user.setUsername(username.trim());
                user.setDisplayName(displayName.trim());
                user.setStatus("ACTIVE");
                user.setPasswordHash(PasswordUtil.hash(password));
                long id = userDao.insert(connection, user);
                userDao.replaceRoles(connection, id, roleIds);
                auditService.record(connection, actor, "USER_CREATE", "USER", String.valueOf(id),
                        true, "新增用户 " + username.trim() + "（" + displayName.trim() + "）", traceId);
                connection.commit();
                return id;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void update(SessionUser actor, long id, String displayName, String status,
                       List<Long> roleIds, String traceId) throws SQLException {
        requireManagePermission(actor);
        Map<String, String> errors = ValidationUtil.newErrors();
        ValidationUtil.requireText(errors, "displayName", "显示名称", displayName, 100);
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            errors.put("status", "状态取值不合法");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            errors.put("roleIds", "至少分配一个角色");
        }
        ValidationUtil.throwIfAny(errors, "请检查用户表单");

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                User existing = userDao.findById(connection, id)
                        .orElseThrow(() -> new NotFoundException("用户不存在"));
                if (id == actor.getId() && !"ACTIVE".equals(status)) {
                    throw new BusinessException("不能停用当前登录账号");
                }
                if (id == actor.getId() && !roleIds.containsAll(actorRoleIds(existing))) {
                    throw new BusinessException("不能移除自己当前拥有的角色");
                }
                userDao.updateProfile(connection, id, displayName.trim(), status);
                userDao.replaceRoles(connection, id, roleIds);
                auditService.record(connection, actor, "USER_UPDATE", "USER", String.valueOf(id),
                        true, "更新用户 " + existing.getUsername() + "，状态 " + status, traceId);
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void setStatus(SessionUser actor, long id, String status, String traceId)
            throws SQLException {
        requireManagePermission(actor);
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException("状态取值不合法");
        }
        if (id == actor.getId() && "DISABLED".equals(status)) {
            throw new BusinessException("不能停用当前登录账号");
        }
        try (Connection connection = Db.getConnection()) {
            User target = userDao.findById(connection, id)
                    .orElseThrow(() -> new NotFoundException("用户不存在"));
            userDao.setStatus(connection, id, status);
            auditService.record(connection, actor, "USER_STATUS", "USER", String.valueOf(id),
                    true, "用户 " + target.getUsername() + " 状态改为 " + status, traceId);
        }
    }

    public void changeOwnPassword(SessionUser actor, String oldPassword, String newPassword,
                                  String traceId) throws SQLException {
        if (!PasswordUtil.isStrongEnough(newPassword)) {
            throw new BusinessException("新密码至少 8 位，且必须同时包含字母和数字");
        }
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        try (Connection connection = Db.getConnection()) {
            User user = userDao.findById(connection, actor.getId())
                    .orElseThrow(() -> new NotFoundException("用户不存在"));
            if (!PasswordUtil.matches(oldPassword, user.getPasswordHash())) {
                throw new BusinessException("原密码不正确");
            }
            userDao.updatePassword(connection, actor.getId(), PasswordUtil.hash(newPassword));
            auditService.record(connection, actor, "USER_CHANGE_PASSWORD", "USER",
                    String.valueOf(actor.getId()), true, "修改本人密码", traceId);
        }
    }

    public String resetPassword(SessionUser actor, long id, String requestedPassword,
                                String traceId) throws SQLException {
        requireManagePermission(actor);
        String newPassword = requestedPassword == null || requestedPassword.trim().isEmpty()
                ? PasswordUtil.generateTemporaryPassword() : requestedPassword.trim();
        if (!PasswordUtil.isStrongEnough(newPassword)) {
            throw new BusinessException("新密码至少 8 位，且必须同时包含字母和数字");
        }
        try (Connection connection = Db.getConnection()) {
            User target = userDao.findById(connection, id)
                    .orElseThrow(() -> new NotFoundException("用户不存在"));
            userDao.updatePassword(connection, id, PasswordUtil.hash(newPassword));
            auditService.record(connection, actor, "USER_RESET_PASSWORD", "USER",
                    String.valueOf(id), true, "重置用户密码 " + target.getUsername(), traceId);
        }
        return newPassword;
    }

    private List<Long> actorRoleIds(User user) {
        List<Long> ids = new ArrayList<>();
        for (Role role : user.getRoles()) {
            ids.add(role.getId());
        }
        return ids;
    }

    private void requireManagePermission(SessionUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.USER_MANAGE)) {
            throw new ForbiddenException("无权管理用户");
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // rollback failure is logged by the caller's 500 page
        }
    }
}
