package com.example.tax.service;

import com.example.tax.dao.PermissionDao;
import com.example.tax.dao.RoleDao;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.Permission;
import com.example.tax.model.Role;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class RoleService {

    private final RoleDao roleDao = new RoleDao();
    private final PermissionDao permissionDao = new PermissionDao();
    private final AuditService auditService = new AuditService();

    public List<Role> listAll() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return roleDao.listAll(connection);
        }
    }

    public List<Permission> listPermissions() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return permissionDao.listAll(connection);
        }
    }

    public Role findById(long id) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            Role role = roleDao.findById(connection, id);
            if (role == null) {
                throw new NotFoundException("角色不存在");
            }
            role.setPermissionIds(roleDao.listPermissionIds(connection, id));
            return role;
        }
    }

    public void save(SessionUser actor, Long id, String roleCode, String roleName,
                     String description, String status, List<Long> permissionIds, String traceId)
            throws SQLException {
        requireManagePermission(actor);
        Map<String, String> errors = ValidationUtil.newErrors();
        if (id == null) {
            ValidationUtil.requireCode(errors, "roleCode", "角色编码", roleCode);
        }
        ValidationUtil.requireText(errors, "roleName", "角色名称", roleName, 100);
        ValidationUtil.optionalText(errors, "description", "说明", description, 255);
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            errors.put("status", "状态取值不合法");
        }
        ValidationUtil.throwIfAny(errors, "请检查角色表单");

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (id == null) {
                    if (roleDao.findByCode(connection, roleCode.trim()) != null) {
                        throw new ValidationException("角色编码已存在",
                                Map.of("roleCode", "该角色编码已被使用"));
                    }
                    Role role = new Role();
                    role.setRoleCode(roleCode.trim());
                    role.setRoleName(roleName.trim());
                    role.setDescription(description == null ? "" : description.trim());
                    role.setStatus(status);
                    long newId = roleDao.insert(connection, role);
                    roleDao.replacePermissions(connection, newId,
                            permissionIds == null ? List.of() : permissionIds);
                    auditService.record(connection, actor, "ROLE_CREATE", "ROLE",
                            String.valueOf(newId), true, "新增角色 " + roleCode.trim(), traceId);
                } else {
                    Role existing = roleDao.findById(connection, id);
                    if (existing == null) {
                        throw new NotFoundException("角色不存在");
                    }
                    if ("ADMIN".equals(existing.getRoleCode()) && !"ACTIVE".equals(status)) {
                        throw new BusinessException("系统管理员角色不能停用");
                    }
                    existing.setRoleName(roleName.trim());
                    existing.setDescription(description == null ? "" : description.trim());
                    existing.setStatus(status);
                    roleDao.update(connection, existing);
                    roleDao.replacePermissions(connection, id,
                            permissionIds == null ? List.of() : permissionIds);
                    auditService.record(connection, actor, "ROLE_UPDATE", "ROLE",
                            String.valueOf(id), true, "更新角色 " + existing.getRoleCode()
                                    + "，权限数 " + (permissionIds == null ? 0 : permissionIds.size()),
                            traceId);
                }
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
        try (Connection connection = Db.getConnection()) {
            Role role = roleDao.findById(connection, id);
            if (role == null) {
                throw new NotFoundException("角色不存在");
            }
            if ("ADMIN".equals(role.getRoleCode()) && "DISABLED".equals(status)) {
                throw new BusinessException("系统管理员角色不能停用");
            }
            roleDao.setStatus(connection, id, status);
            auditService.record(connection, actor, "ROLE_STATUS", "ROLE", String.valueOf(id),
                    true, "角色 " + role.getRoleCode() + " 状态改为 " + status, traceId);
        }
    }

    private void requireManagePermission(SessionUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.ROLE_MANAGE)) {
            throw new ForbiddenException("无权管理角色权限");
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // caller surfaces a safe 500 page
        }
    }
}
