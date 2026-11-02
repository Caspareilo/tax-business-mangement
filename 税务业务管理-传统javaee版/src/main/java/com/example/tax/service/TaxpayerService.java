package com.example.tax.service;

import com.example.tax.dao.TaxpayerDao;
import com.example.tax.dto.Page;
import com.example.tax.dto.TaxpayerFilter;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.Taxpayer;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TaxpayerService {

    private final TaxpayerDao taxpayerDao = new TaxpayerDao();
    private final AuditService auditService = new AuditService();

    public Page<Taxpayer> list(TaxpayerFilter filter, int page, int size) throws SQLException {
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = taxpayerDao.count(connection, filter);
            List<Taxpayer> rows = taxpayerDao.search(connection, filter,
                    (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public Taxpayer findById(long id) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            Taxpayer taxpayer = taxpayerDao.findById(connection, id);
            if (taxpayer == null) {
                throw new NotFoundException("纳税人不存在");
            }
            return taxpayer;
        }
    }

    public List<Taxpayer> listActive() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return taxpayerDao.listActive(connection);
        }
    }

    public long create(SessionUser actor, Taxpayer input, String traceId) throws SQLException {
        requireManagePermission(actor);
        validate(input);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (taxpayerDao.findByCode(connection, input.getTaxpayerCode()) != null) {
                    throw new ValidationException("纳税人编号已存在",
                            Map.of("taxpayerCode", "该纳税人编号已被使用"));
                }
                long id = taxpayerDao.insert(connection, input);
                auditService.record(connection, actor, "TAXPAYER_CREATE", "TAXPAYER",
                        String.valueOf(id), true,
                        "新增纳税人 " + input.getTaxpayerCode() + " " + input.getTaxpayerName(),
                        traceId);
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

    public void update(SessionUser actor, Taxpayer input, String traceId) throws SQLException {
        requireManagePermission(actor);
        validate(input);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Taxpayer existing = taxpayerDao.findById(connection, input.getId());
                if (existing == null) {
                    throw new NotFoundException("纳税人不存在");
                }
                input.setTaxpayerCode(existing.getTaxpayerCode());
                int rows = taxpayerDao.update(connection, input);
                if (rows == 0) {
                    throw new BusinessException("纳税人信息已变化，请重新加载后修改");
                }
                auditService.record(connection, actor, "TAXPAYER_UPDATE", "TAXPAYER",
                        String.valueOf(input.getId()), true,
                        "更新纳税人 " + existing.getTaxpayerCode() + " " + input.getTaxpayerName(),
                        traceId);
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
            Taxpayer taxpayer = taxpayerDao.findById(connection, id);
            if (taxpayer == null) {
                throw new NotFoundException("纳税人不存在");
            }
            taxpayerDao.setStatus(connection, id, status);
            auditService.record(connection, actor, "TAXPAYER_STATUS", "TAXPAYER",
                    String.valueOf(id), true, "纳税人 " + taxpayer.getTaxpayerCode()
                            + " 状态改为 " + status, traceId);
        }
    }

    private void validate(Taxpayer input) {
        Map<String, String> errors = ValidationUtil.newErrors();
        ValidationUtil.requireCode(errors, "taxpayerCode", "纳税人编号", input.getTaxpayerCode());
        ValidationUtil.requireText(errors, "taxpayerName", "纳税人名称", input.getTaxpayerName(),
                200);
        if (!"COMPANY".equals(input.getTaxpayerType())
                && !"INDIVIDUAL".equals(input.getTaxpayerType())
                && !"INSTITUTION".equals(input.getTaxpayerType())) {
            errors.put("taxpayerType", "请选择有效的主体类型");
        }
        ValidationUtil.optionalText(errors, "contactName", "联系人", input.getContactName(), 100);
        ValidationUtil.optionalText(errors, "contactPhone", "联系电话", input.getContactPhone(), 40);
        ValidationUtil.optionalText(errors, "remark", "备注", input.getRemark(), 500);
        if (!"ACTIVE".equals(input.getRegistrationStatus())
                && !"DISABLED".equals(input.getRegistrationStatus())) {
            errors.put("registrationStatus", "登记状态取值不合法");
        }
        ValidationUtil.throwIfAny(errors, "请检查纳税人表单");
    }

    private void requireManagePermission(SessionUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.TAXPAYER_MANAGE)) {
            throw new ForbiddenException("无权维护纳税人信息");
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
