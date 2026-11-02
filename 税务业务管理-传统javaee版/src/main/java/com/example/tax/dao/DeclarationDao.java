package com.example.tax.dao;

import com.example.tax.dto.DeclarationFilter;
import com.example.tax.dto.MonthlyPoint;
import com.example.tax.dto.StatusCount;
import com.example.tax.dto.TypeAmount;
import com.example.tax.model.TaxDeclaration;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeclarationDao {

    private static final String SELECT_BASE =
            "SELECT d.*, u.display_name AS created_by_name, t.taxpayer_code AS taxpayer_code "
                    + "FROM tax_declaration d "
                    + "LEFT JOIN sys_user u ON u.id = d.created_by "
                    + "LEFT JOIN taxpayer t ON t.id = d.taxpayer_id ";

    public long insert(Connection connection, TaxDeclaration declaration) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO tax_declaration (declaration_no, taxpayer_id, taxpayer_name_snapshot, "
                        + "declaration_type_id, declaration_type_snapshot, template_id, "
                        + "template_version_no, period_start, period_end, status, declared_amount, "
                        + "form_data_json, created_by, submitted_at, last_reviewed_at, version, "
                        + "created_at, updated_at, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, 0, "
                        + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                declaration.getDeclarationNo(), declaration.getTaxpayerId(),
                declaration.getTaxpayerNameSnapshot(), declaration.getDeclarationTypeId(),
                declaration.getDeclarationTypeSnapshot(), declaration.getTemplateId(),
                declaration.getTemplateVersionNo(), date(declaration.getPeriodStart()),
                date(declaration.getPeriodEnd()), declaration.getStatus(),
                declaration.getDeclaredAmount(), declaration.getFormDataJson(),
                declaration.getCreatedBy());
    }

    public int updateDraft(Connection connection, TaxDeclaration declaration) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE tax_declaration SET taxpayer_id = ?, taxpayer_name_snapshot = ?, "
                        + "declaration_type_id = ?, declaration_type_snapshot = ?, template_id = ?, "
                        + "template_version_no = ?, period_start = ?, period_end = ?, "
                        + "declared_amount = ?, form_data_json = ?, version = version + 1, "
                        + "updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND created_by = ? AND status IN ('DRAFT', 'RETURNED') "
                        + "AND deleted = 0",
                declaration.getTaxpayerId(), declaration.getTaxpayerNameSnapshot(),
                declaration.getDeclarationTypeId(), declaration.getDeclarationTypeSnapshot(),
                declaration.getTemplateId(), declaration.getTemplateVersionNo(),
                date(declaration.getPeriodStart()), date(declaration.getPeriodEnd()),
                declaration.getDeclaredAmount(), declaration.getFormDataJson(),
                declaration.getId(), declaration.getCreatedBy());
    }

    public int submit(Connection connection, long id, long createdBy, BigDecimal amount)
            throws SQLException {
        return Jdbc.update(connection,
                "UPDATE tax_declaration SET status = 'PENDING_REVIEW', "
                        + "declared_amount = ?, submitted_at = CURRENT_TIMESTAMP, "
                        + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND created_by = ? AND deleted = 0 "
                        + "AND status IN ('DRAFT', 'RETURNED')",
                amount, id, createdBy);
    }

    public int withdraw(Connection connection, long id, long userId, boolean asAdmin)
            throws SQLException {
        return Jdbc.update(connection,
                "UPDATE tax_declaration SET status = 'WITHDRAWN', version = version + 1, "
                        + "updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND (created_by = ? OR ? = 1) AND deleted = 0 "
                        + "AND status = 'PENDING_REVIEW' "
                        + "AND NOT EXISTS (SELECT 1 FROM declaration_review r "
                        + "WHERE r.declaration_id = tax_declaration.id)",
                id, userId, asAdmin ? 1 : 0);
    }

    public int markReviewed(Connection connection, long id, String toStatus) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE tax_declaration SET status = ?, last_reviewed_at = CURRENT_TIMESTAMP, "
                        + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND status = 'PENDING_REVIEW' AND deleted = 0",
                toStatus, id);
    }

    public TaxDeclaration findById(Connection connection, long id) throws SQLException {
        return Jdbc.queryOne(connection, SELECT_BASE + " WHERE d.id = ? AND d.deleted = 0",
                this::map, id);
    }

    public List<TaxDeclaration> search(Connection connection, DeclarationFilter filter,
                                       int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE + " WHERE d.deleted = 0");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        sql.append(" ORDER BY ").append(sortClause(filter == null ? null : filter.getSort()));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return Jdbc.query(connection, sql.toString(), this::map, params.toArray());
    }

    public long count(Connection connection, DeclarationFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM tax_declaration d "
                        + "LEFT JOIN sys_user u ON u.id = d.created_by "
                        + "LEFT JOIN taxpayer t ON t.id = d.taxpayer_id WHERE d.deleted = 0");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        return Jdbc.queryCount(connection, sql.toString(), params.toArray());
    }

    public List<TaxDeclaration> findPending(Connection connection, int offset, int limit)
            throws SQLException {
        return Jdbc.query(connection,
                SELECT_BASE + " WHERE d.status = 'PENDING_REVIEW' AND d.deleted = 0 "
                        + "ORDER BY d.submitted_at ASC, d.id ASC LIMIT ? OFFSET ?",
                this::map, limit, offset);
    }

    public long countPending(Connection connection) throws SQLException {
        return Jdbc.queryCount(connection,
                "SELECT COUNT(*) FROM tax_declaration WHERE status = 'PENDING_REVIEW' AND deleted = 0");
    }

    public boolean existsActiveDuplicate(Connection connection, long taxpayerId, long typeId,
                                         LocalDate periodStart, LocalDate periodEnd, Long excludeId)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM tax_declaration WHERE deleted = 0 "
                        + "AND taxpayer_id = ? AND declaration_type_id = ? "
                        + "AND status IN ('PENDING_REVIEW', 'APPROVED') "
                        + "AND period_start <= ? AND period_end >= ?");
        List<Object> params = new ArrayList<>();
        params.add(taxpayerId);
        params.add(typeId);
        params.add(date(periodEnd));
        params.add(date(periodStart));
        if (excludeId != null) {
            sql.append(" AND id <> ?");
            params.add(excludeId);
        }
        return Jdbc.queryCount(connection, sql.toString(), params.toArray()) > 0;
    }

    public Map<String, Long> countByStatus(Connection connection) throws SQLException {
        return countByStatus(connection, null);
    }

    public Map<String, Long> countByStatus(Connection connection, Long creatorId)
            throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS cnt FROM tax_declaration WHERE deleted = 0"
                + (creatorId == null ? "" : " AND created_by = ?")
                + " GROUP BY status";
        List<StatusCount> rows = Jdbc.query(connection, sql,
                rs -> new StatusCount(rs.getString("status"), rs.getString("status"),
                        rs.getLong("cnt")),
                creatorId == null ? new Object[0] : new Object[]{creatorId});
        for (StatusCount row : rows) {
            counts.put(row.getStatus(), row.getCount());
        }
        return counts;
    }

    public BigDecimal sumAmount(Connection connection) throws SQLException {
        return sumAmount(connection, null);
    }

    public BigDecimal sumAmount(Connection connection, Long creatorId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(declared_amount), 0) FROM tax_declaration "
                + "WHERE deleted = 0 AND status <> 'WITHDRAWN'"
                + (creatorId == null ? "" : " AND created_by = ?");
        BigDecimal sum = Jdbc.queryOne(connection, sql, rs -> rs.getBigDecimal(1),
                creatorId == null ? new Object[0] : new Object[]{creatorId});
        return sum == null ? BigDecimal.ZERO : sum;
    }

    public List<MonthlyPoint> monthlyTrend(Connection connection, LocalDate fromMonth) throws SQLException {
        return monthlyTrend(connection, fromMonth, null);
    }

    public List<MonthlyPoint> monthlyTrend(Connection connection, LocalDate fromMonth,
                                           Long creatorId) throws SQLException {
        Timestamp from = Timestamp.valueOf(fromMonth.withDayOfMonth(1).atStartOfDay());
        String sql = "SELECT YEAR(created_at) AS y, MONTH(created_at) AS m, COUNT(*) AS cnt, "
                + "COALESCE(SUM(declared_amount), 0) AS amount "
                + "FROM tax_declaration WHERE deleted = 0 AND created_at >= ?"
                + (creatorId == null ? "" : " AND created_by = ?")
                + " GROUP BY YEAR(created_at), MONTH(created_at) ORDER BY y, m";
        Object[] params = creatorId == null ? new Object[]{from} : new Object[]{from, creatorId};
        return Jdbc.query(connection, sql,
                rs -> new MonthlyPoint(String.format("%04d-%02d",
                        rs.getInt("y"), rs.getInt("m")),
                        rs.getLong("cnt"), rs.getBigDecimal("amount")), params);
    }

    public List<TypeAmount> amountByType(Connection connection) throws SQLException {
        return amountByType(connection, null);
    }

    public List<TypeAmount> amountByType(Connection connection, Long creatorId)
            throws SQLException {
        String sql = "SELECT dt.type_name AS label, COUNT(*) AS cnt, "
                + "COALESCE(SUM(d.declared_amount), 0) AS amount "
                + "FROM tax_declaration d "
                + "JOIN declaration_type dt ON dt.id = d.declaration_type_id "
                + "WHERE d.deleted = 0 AND d.status <> 'WITHDRAWN'"
                + (creatorId == null ? "" : " AND d.created_by = ?")
                + " GROUP BY dt.id, dt.type_name ORDER BY amount DESC";
        return Jdbc.query(connection, sql,
                rs -> new TypeAmount(rs.getString("label"), rs.getLong("cnt"),
                        rs.getBigDecimal("amount")),
                creatorId == null ? new Object[0] : new Object[]{creatorId});
    }

    public List<TypeAmount> amountByTaxpayerType(Connection connection) throws SQLException {
        return amountByTaxpayerType(connection, null);
    }

    public List<TypeAmount> amountByTaxpayerType(Connection connection, Long creatorId)
            throws SQLException {
        String sql = "SELECT t.taxpayer_type AS label, COUNT(*) AS cnt, "
                + "COALESCE(SUM(d.declared_amount), 0) AS amount "
                + "FROM tax_declaration d JOIN taxpayer t ON t.id = d.taxpayer_id "
                + "WHERE d.deleted = 0 AND d.status <> 'WITHDRAWN'"
                + (creatorId == null ? "" : " AND d.created_by = ?")
                + " GROUP BY t.taxpayer_type ORDER BY amount DESC";
        return Jdbc.query(connection, sql,
                rs -> {
                    String type = rs.getString("label");
                    return new TypeAmount(com.example.tax.model.Taxpayer.taxpayerTypeLabel(type),
                            rs.getLong("cnt"), rs.getBigDecimal("amount"));
                },
                creatorId == null ? new Object[0] : new Object[]{creatorId});
    }

    public List<TaxDeclaration> recent(Connection connection, int limit) throws SQLException {
        return Jdbc.query(connection,
                SELECT_BASE + " WHERE d.deleted = 0 ORDER BY d.created_at DESC, d.id DESC LIMIT ?",
                this::map, limit);
    }

    public long countByCreatorAndStatuses(Connection connection, long creatorId, String... statuses)
            throws SQLException {
        if (statuses.length == 0) {
            return 0;
        }
        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();
        params.add(creatorId);
        for (String status : statuses) {
            if (placeholders.length() > 0) {
                placeholders.append(',');
            }
            placeholders.append('?');
            params.add(status);
        }
        return Jdbc.queryCount(connection,
                "SELECT COUNT(*) FROM tax_declaration WHERE deleted = 0 AND created_by = ? "
                        + "AND status IN (" + placeholders + ")", params.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> params, DeclarationFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append(" AND (d.declaration_no LIKE ? OR d.taxpayer_name_snapshot LIKE ? "
                    + "OR u.display_name LIKE ? OR t.taxpayer_code LIKE ?)");
            String like = "%" + filter.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (filter.getDeclarationTypeId() != null) {
            sql.append(" AND d.declaration_type_id = ?");
            params.add(filter.getDeclarationTypeId());
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append(" AND d.status = ?");
            params.add(filter.getStatus());
        }
        if (filter.getPeriodFrom() != null) {
            sql.append(" AND d.period_end >= ?");
            params.add(date(filter.getPeriodFrom()));
        }
        if (filter.getPeriodTo() != null) {
            sql.append(" AND d.period_start <= ?");
            params.add(date(filter.getPeriodTo()));
        }
        if (filter.getSubmittedFrom() != null) {
            sql.append(" AND d.submitted_at >= ?");
            params.add(Timestamp.valueOf(filter.getSubmittedFrom().atStartOfDay()));
        }
        if (filter.getSubmittedTo() != null) {
            sql.append(" AND d.submitted_at < ?");
            params.add(Timestamp.valueOf(filter.getSubmittedTo().plusDays(1).atStartOfDay()));
        }
        if (filter.getCreatedBy() != null) {
            sql.append(" AND d.created_by = ?");
            params.add(filter.getCreatedBy());
        }
        if (filter.getTaxpayerId() != null) {
            sql.append(" AND d.taxpayer_id = ?");
            params.add(filter.getTaxpayerId());
        }
        if (filter.getReviewerId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM declaration_review r "
                    + "WHERE r.declaration_id = d.id AND r.reviewer_id = ?)");
            params.add(filter.getReviewerId());
        }
    }

    private String sortClause(String sort) {
        if (sort == null) {
            return "d.id DESC";
        }
        switch (sort) {
            case "submitted":
                return "d.submitted_at DESC, d.id DESC";
            case "amount":
                return "d.declared_amount DESC, d.id DESC";
            case "number":
                return "d.declaration_no ASC";
            case "period":
                return "d.period_start DESC, d.id DESC";
            case "newest":
            default:
                return "d.id DESC";
        }
    }

    private TaxDeclaration map(ResultSet rs) throws SQLException {
        TaxDeclaration declaration = new TaxDeclaration();
        declaration.setId(rs.getLong("id"));
        declaration.setDeclarationNo(rs.getString("declaration_no"));
        declaration.setTaxpayerId(rs.getLong("taxpayer_id"));
        declaration.setTaxpayerNameSnapshot(rs.getString("taxpayer_name_snapshot"));
        declaration.setTaxpayerCode(rs.getString("taxpayer_code"));
        declaration.setDeclarationTypeId(rs.getLong("declaration_type_id"));
        declaration.setDeclarationTypeSnapshot(rs.getString("declaration_type_snapshot"));
        declaration.setTemplateId(rs.getLong("template_id"));
        declaration.setTemplateVersionNo(rs.getInt("template_version_no"));
        java.sql.Date start = rs.getDate("period_start");
        java.sql.Date end = rs.getDate("period_end");
        declaration.setPeriodStart(start == null ? null : start.toLocalDate());
        declaration.setPeriodEnd(end == null ? null : end.toLocalDate());
        declaration.setStatus(rs.getString("status"));
        declaration.setDeclaredAmount(rs.getBigDecimal("declared_amount"));
        declaration.setFormDataJson(rs.getString("form_data_json"));
        declaration.setCreatedBy(rs.getLong("created_by"));
        declaration.setCreatedByName(rs.getString("created_by_name"));
        declaration.setSubmittedAt(rs.getTimestamp("submitted_at"));
        declaration.setLastReviewedAt(rs.getTimestamp("last_reviewed_at"));
        declaration.setVersion(rs.getInt("version"));
        declaration.setCreatedAt(rs.getTimestamp("created_at"));
        declaration.setUpdatedAt(rs.getTimestamp("updated_at"));
        declaration.setDeleted(rs.getBoolean("deleted"));
        return declaration;
    }

    private static java.sql.Date date(LocalDate value) {
        return value == null ? null : java.sql.Date.valueOf(value);
    }

}
