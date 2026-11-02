package com.example.tax.service;

import com.example.tax.dao.AuditLogDao;
import com.example.tax.dto.AuditLogFilter;
import com.example.tax.dto.Page;
import com.example.tax.model.OperationLog;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogDao auditLogDao = new AuditLogDao();

    /**
     * Writes an audit row inside the caller's transaction. Summary must never include
     * passwords, session ids, tokens or full sensitive form data.
     */
    public void record(Connection connection, SessionUser actor, String operationType,
                       String targetType, String targetId, boolean success, String summary,
                       String traceId) throws SQLException {
        OperationLog log = new OperationLog();
        if (actor != null) {
            log.setOperatorId(actor.getId());
            log.setOperatorNameSnapshot(actor.getDisplayName());
        }
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setResult(success ? "SUCCESS" : "FAILURE");
        log.setSummary(truncate(summary, 500));
        log.setTraceId(truncate(traceId, 100));
        auditLogDao.insert(connection, log);
    }

    public void recordAnonymous(String operatorName, String operationType, String targetType,
                                String targetId, boolean success, String summary, String traceId) {
        try (Connection connection = Db.getConnection()) {
            OperationLog log = new OperationLog();
            log.setOperatorNameSnapshot(truncate(operatorName, 100));
            log.setOperationType(operationType);
            log.setTargetType(targetType);
            log.setTargetId(targetId);
            log.setResult(success ? "SUCCESS" : "FAILURE");
            log.setSummary(truncate(summary, 500));
            log.setTraceId(truncate(traceId, 100));
            auditLogDao.insert(connection, log);
        } catch (SQLException e) {
            LOGGER.warn("写入匿名审计日志失败: {}", e.getMessage());
        }
    }

    /** Best-effort audit write that must not break the main transaction. */
    public void safeRecord(Connection connection, SessionUser actor, String operationType,
                           String targetType, String targetId, String summary, String traceId) {
        try {
            record(connection, actor, operationType, targetType, targetId, true, summary, traceId);
        } catch (SQLException e) {
            LOGGER.warn("写入审计日志失败: {}", e.getMessage());
        }
    }

    public Page<OperationLog> search(AuditLogFilter filter, int page, int size) throws SQLException {
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = auditLogDao.count(connection, filter);
            List<OperationLog> rows = auditLogDao.search(connection, filter,
                    (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public List<String> operationTypes() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return auditLogDao.listOperationTypes(connection);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
