package com.example.tax.service;

import com.example.tax.dao.DeclarationTypeDao;
import com.example.tax.dto.Page;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.DeclarationType;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DeclarationTypeService {

    private final DeclarationTypeDao typeDao = new DeclarationTypeDao();
    private final AuditService auditService = new AuditService();

    public Page<DeclarationType> list(String keyword, String enabled, int page, int size)
            throws SQLException {
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = typeDao.count(connection, keyword, enabled);
            List<DeclarationType> rows = typeDao.search(connection, keyword, enabled,
                    (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public List<DeclarationType> listAll() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return typeDao.listAll(connection);
        }
    }

    public List<DeclarationType> listEnabled() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return typeDao.listEnabled(connection);
        }
    }

    public DeclarationType findById(long id) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            DeclarationType type = typeDao.findById(connection, id);
            if (type == null) {
                throw new NotFoundException("申报类型不存在");
            }
            return type;
        }
    }

    public void save(SessionUser actor, Long id, String typeCode, String typeName,
                     String description, Integer deadlineDay, String traceId) throws SQLException {
        requireManagePermission(actor);
        Map<String, String> errors = ValidationUtil.newErrors();
        if (id == null) {
            ValidationUtil.requireCode(errors, "typeCode", "类型编码", typeCode);
        }
        ValidationUtil.requireText(errors, "typeName", "类型名称", typeName, 100);
        ValidationUtil.optionalText(errors, "description", "说明", description, 500);
        if (deadlineDay == null || deadlineDay < 1 || deadlineDay > 28) {
            errors.put("deadlineDay", "模拟申报期限须为 1-28 日");
        }
        ValidationUtil.throwIfAny(errors, "请检查申报类型表单");

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (id == null) {
                    if (typeDao.findByCode(connection, typeCode.trim()) != null) {
                        throw new ValidationException("类型编码已存在",
                                Map.of("typeCode", "该类型编码已被使用"));
                    }
                    DeclarationType type = new DeclarationType();
                    type.setTypeCode(typeCode.trim());
                    type.setTypeName(typeName.trim());
                    type.setDescription(description == null ? "" : description.trim());
                    type.setDeadlineDay(deadlineDay);
                    type.setEnabled(true);
                    long newId = typeDao.insert(connection, type);
                    auditService.record(connection, actor, "TYPE_CREATE", "DECLARATION_TYPE",
                            String.valueOf(newId), true, "新增申报类型 " + typeCode.trim(), traceId);
                } else {
                    DeclarationType existing = typeDao.findById(connection, id);
                    if (existing == null) {
                        throw new NotFoundException("申报类型不存在");
                    }
                    existing.setTypeName(typeName.trim());
                    existing.setDescription(description == null ? "" : description.trim());
                    existing.setDeadlineDay(deadlineDay);
                    typeDao.update(connection, existing);
                    auditService.record(connection, actor, "TYPE_UPDATE", "DECLARATION_TYPE",
                            String.valueOf(id), true,
                            "更新申报类型 " + existing.getTypeCode(), traceId);
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

    public void setEnabled(SessionUser actor, long id, boolean enabled, String traceId)
            throws SQLException {
        requireManagePermission(actor);
        try (Connection connection = Db.getConnection()) {
            DeclarationType type = typeDao.findById(connection, id);
            if (type == null) {
                throw new NotFoundException("申报类型不存在");
            }
            typeDao.setEnabled(connection, id, enabled);
            auditService.record(connection, actor, "TYPE_STATUS", "DECLARATION_TYPE",
                    String.valueOf(id), true, "申报类型 " + type.getTypeCode()
                            + (enabled ? " 已启用" : " 已停用"), traceId);
        }
    }

    private void requireManagePermission(SessionUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.TYPE_MANAGE)) {
            throw new ForbiddenException("无权维护申报类型");
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // handled by servlet error page
        }
    }
}
