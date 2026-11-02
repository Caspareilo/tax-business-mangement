package com.example.tax.dao;

import com.example.tax.dto.AuditLogFilter;
import com.example.tax.model.OperationLog;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {

    public long insert(Connection connection, OperationLog log) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO sys_operation_log (operator_id, operator_name_snapshot, "
                        + "operation_type, target_type, target_id, result, summary, trace_id, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                log.getOperatorId(), log.getOperatorNameSnapshot(), log.getOperationType(),
                log.getTargetType(), log.getTargetId(), log.getResult(), log.getSummary(),
                log.getTraceId());
    }

    public List<OperationLog> search(Connection connection, AuditLogFilter filter,
                                     int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_operation_log WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return Jdbc.query(connection, sql.toString(), this::map, params.toArray());
    }

    public long count(Connection connection, AuditLogFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_operation_log WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        return Jdbc.queryCount(connection, sql.toString(), params.toArray());
    }

    public List<String> listOperationTypes(Connection connection) throws SQLException {
        return Jdbc.query(connection,
                "SELECT DISTINCT operation_type FROM sys_operation_log ORDER BY operation_type",
                rs -> rs.getString(1));
    }

    private void appendFilters(StringBuilder sql, List<Object> params, AuditLogFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append(" AND (operation_type LIKE ? OR operator_name_snapshot LIKE ? "
                    + "OR summary LIKE ? OR target_id LIKE ?)");
            String like = "%" + filter.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (filter.getOperationType() != null && !filter.getOperationType().isEmpty()) {
            sql.append(" AND operation_type = ?");
            params.add(filter.getOperationType());
        }
        if (filter.getResult() != null && !filter.getResult().isEmpty()) {
            sql.append(" AND result = ?");
            params.add(filter.getResult());
        }
        if (filter.getDateFrom() != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.valueOf(filter.getDateFrom().atStartOfDay()));
        }
        if (filter.getDateTo() != null) {
            sql.append(" AND created_at < ?");
            params.add(Timestamp.valueOf(filter.getDateTo().plusDays(1).atStartOfDay()));
        }
    }

    private OperationLog map(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.setId(rs.getLong("id"));
        long operatorId = rs.getLong("operator_id");
        log.setOperatorId(rs.wasNull() ? null : operatorId);
        log.setOperatorNameSnapshot(rs.getString("operator_name_snapshot"));
        log.setOperationType(rs.getString("operation_type"));
        log.setTargetType(rs.getString("target_type"));
        log.setTargetId(rs.getString("target_id"));
        log.setResult(rs.getString("result"));
        log.setSummary(rs.getString("summary"));
        log.setTraceId(rs.getString("trace_id"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        return log;
    }
}
