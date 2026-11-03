package com.example.tax.service;

import com.example.tax.dao.PermissionDao;
import com.example.tax.dao.RoleDao;
import com.example.tax.dao.UserDao;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.Role;
import com.example.tax.model.User;
import com.example.tax.security.PasswordUtil;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthService {

    private final UserDao userDao = new UserDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final RoleDao roleDao = new RoleDao();
    private final AuditService auditService = new AuditService();

    public SessionUser login(String username, String password, String traceId) throws SQLException {
        String trimmedUsername = username == null ? "" : username.trim();
        try (Connection connection = Db.getConnection()) {
            Optional<User> optionalUser = userDao.findByUsername(connection, trimmedUsername);
            if (!optionalUser.isPresent()
                    || !PasswordUtil.matches(password, optionalUser.get().getPasswordHash())) {
                auditService.record(connection, null, "LOGIN", "SESSION", null, false,
                        "登录失败：账号 " + trimmedUsername, traceId);
                throw new BusinessException("账号或密码错误");
            }
            User user = optionalUser.get();
            if (!user.isActive()) {
                auditService.record(connection, null, "LOGIN", "SESSION", String.valueOf(user.getId()),
                        false, "登录失败：账号已停用 " + trimmedUsername, traceId);
                throw new BusinessException("账号已停用，请联系系统管理员");
            }
            List<String> roleCodes = permissionDao.listRoleCodesByUserId(connection, user.getId());
            if (roleCodes.isEmpty()) {
                auditService.record(connection, null, "LOGIN", "SESSION", String.valueOf(user.getId()),
                        false, "登录失败：账号无有效角色 " + trimmedUsername, traceId);
                throw new BusinessException("账号未分配有效角色，请联系系统管理员");
            }
            List<String> permissionCodes =
                    permissionDao.listCodesByUserId(connection, user.getId());
            userDao.updateLastLogin(connection, user.getId());
            SessionUser sessionUser = new SessionUser(user.getId(), user.getUsername(),
                    user.getDisplayName(), new java.util.LinkedHashSet<>(roleCodes),
                    new java.util.LinkedHashSet<>(permissionCodes));
            auditService.record(connection, sessionUser, "LOGIN", "SESSION",
                    String.valueOf(user.getId()), true, "登录成功", traceId);
            return sessionUser;
        }
    }

    public void changeOwnPassword(long userId, String oldPassword, String newPassword)
            throws SQLException {
        if (!PasswordUtil.isStrongEnough(newPassword)) {
            throw new BusinessException("新密码至少 8 位，且必须同时包含字母和数字");
        }
        try (Connection connection = Db.getConnection()) {
            User user = userDao.findById(connection, userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
            if (!PasswordUtil.matches(oldPassword, user.getPasswordHash())) {
                throw new BusinessException("原密码不正确");
            }
            if (PasswordUtil.matches(newPassword, user.getPasswordHash())) {
                throw new BusinessException("新密码不能与原密码相同");
            }
            userDao.updatePassword(connection, userId, PasswordUtil.hash(newPassword));
        }
    }

    /**
     * Public self-registration always creates an ACTIVE user with the existing
     * VIEWER role. Elevated roles can only be assigned later by an administrator.
     */
    public long register(String username, String displayName, String password,
                         String confirmPassword, String traceId) throws SQLException {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanDisplayName = displayName == null ? "" : displayName.trim();
        Map<String, String> errors = ValidationUtil.newErrors();
        ValidationUtil.requireUsername(errors, "username", "登录账号", cleanUsername);
        ValidationUtil.requireText(errors, "displayName", "显示名称", cleanDisplayName, 100);
        if (!PasswordUtil.isStrongEnough(password)) {
            errors.put("password", "密码至少 8 位，且必须同时包含字母和数字");
        }
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            errors.put("confirmPassword", "两次输入的密码不一致");
        }
        ValidationUtil.throwIfAny(errors, "请检查注册信息");

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (userDao.findByUsername(connection, cleanUsername).isPresent()) {
                    throw new ValidationException("登录账号已存在",
                            Map.of("username", "该登录账号已被使用"));
                }
                Role viewerRole = roleDao.findByCode(connection, "VIEWER");
                if (viewerRole == null || !"ACTIVE".equals(viewerRole.getStatus())) {
                    throw new BusinessException("系统暂未开放注册，请联系管理员");
                }

                User user = new User();
                user.setUsername(cleanUsername);
                user.setDisplayName(cleanDisplayName);
                user.setStatus("ACTIVE");
                user.setPasswordHash(PasswordUtil.hash(password));
                try {
                    long userId = userDao.insert(connection, user);
                    userDao.replaceRoles(connection, userId, List.of(viewerRole.getId()));
                    auditService.record(connection, null, "USER_REGISTER", "USER",
                            String.valueOf(userId), true,
                            "自助注册只读账号 " + cleanUsername, traceId);
                    connection.commit();
                    return userId;
                } catch (SQLException e) {
                    if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                        throw new ValidationException("登录账号已存在",
                                Map.of("username", "该登录账号已被使用"));
                    }
                    throw e;
                }
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The caller renders a safe server error page.
        }
    }
}
