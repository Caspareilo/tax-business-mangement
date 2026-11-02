package com.example.tax.dao;

import com.example.tax.dto.TaxpayerFilter;
import com.example.tax.model.Taxpayer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaxpayerDao {

    public List<Taxpayer> search(Connection connection, TaxpayerFilter filter, int offset, int limit)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, (SELECT COUNT(*) FROM tax_declaration d "
                        + "WHERE d.taxpayer_id = t.id AND d.deleted = 0) AS declaration_count "
                        + "FROM taxpayer t WHERE t.deleted = 0");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        sql.append(" ORDER BY t.id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return Jdbc.query(connection, sql.toString(), this::map, params.toArray());
    }

    public long count(Connection connection, TaxpayerFilter filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM taxpayer t WHERE t.deleted = 0");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, filter);
        return Jdbc.queryCount(connection, sql.toString(), params.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> params, TaxpayerFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            sql.append(" AND (t.taxpayer_code LIKE ? OR t.taxpayer_name LIKE ?)");
            String like = "%" + filter.getKeyword().trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (filter.getTaxpayerType() != null && !filter.getTaxpayerType().isEmpty()) {
            sql.append(" AND t.taxpayer_type = ?");
            params.add(filter.getTaxpayerType());
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append(" AND t.registration_status = ?");
            params.add(filter.getStatus());
        }
    }

    public Taxpayer findById(Connection connection, long id) throws SQLException {
        return Jdbc.queryOne(connection,
                "SELECT t.*, (SELECT COUNT(*) FROM tax_declaration d "
                        + "WHERE d.taxpayer_id = t.id AND d.deleted = 0) AS declaration_count "
                        + "FROM taxpayer t WHERE t.id = ? AND t.deleted = 0",
                this::map, id);
    }

    public Taxpayer findByCode(Connection connection, String code) throws SQLException {
        return Jdbc.queryOne(connection,
                "SELECT t.*, 0 AS declaration_count FROM taxpayer t "
                        + "WHERE t.taxpayer_code = ? AND t.deleted = 0",
                this::map, code);
    }

    public List<Taxpayer> listActive(Connection connection) throws SQLException {
        return Jdbc.query(connection,
                "SELECT t.*, 0 AS declaration_count FROM taxpayer t "
                        + "WHERE t.deleted = 0 AND t.registration_status = 'ACTIVE' "
                        + "ORDER BY t.taxpayer_code",
                this::map);
    }

    public long insert(Connection connection, Taxpayer taxpayer) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO taxpayer (taxpayer_code, taxpayer_name, taxpayer_type, contact_name, "
                        + "contact_phone, registration_status, remark, created_at, updated_at, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                taxpayer.getTaxpayerCode(), taxpayer.getTaxpayerName(), taxpayer.getTaxpayerType(),
                taxpayer.getContactName(), taxpayer.getContactPhone(),
                taxpayer.getRegistrationStatus(), taxpayer.getRemark());
    }

    public int update(Connection connection, Taxpayer taxpayer) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE taxpayer SET taxpayer_name = ?, taxpayer_type = ?, contact_name = ?, "
                        + "contact_phone = ?, registration_status = ?, remark = ?, "
                        + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0 AND taxpayer_code = ?",
                taxpayer.getTaxpayerName(), taxpayer.getTaxpayerType(), taxpayer.getContactName(),
                taxpayer.getContactPhone(), taxpayer.getRegistrationStatus(), taxpayer.getRemark(),
                taxpayer.getId(), taxpayer.getTaxpayerCode());
    }

    public int setStatus(Connection connection, long id, String status) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE taxpayer SET registration_status = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND deleted = 0",
                status, id);
    }

    public long countDeclarations(Connection connection, long taxpayerId) throws SQLException {
        return Jdbc.queryCount(connection,
                "SELECT COUNT(*) FROM tax_declaration WHERE taxpayer_id = ? AND deleted = 0",
                taxpayerId);
    }

    private Taxpayer map(ResultSet rs) throws SQLException {
        Taxpayer taxpayer = new Taxpayer();
        taxpayer.setId(rs.getLong("id"));
        taxpayer.setTaxpayerCode(rs.getString("taxpayer_code"));
        taxpayer.setTaxpayerName(rs.getString("taxpayer_name"));
        taxpayer.setTaxpayerType(rs.getString("taxpayer_type"));
        taxpayer.setContactName(rs.getString("contact_name"));
        taxpayer.setContactPhone(rs.getString("contact_phone"));
        taxpayer.setRegistrationStatus(rs.getString("registration_status"));
        taxpayer.setRemark(rs.getString("remark"));
        taxpayer.setCreatedAt(rs.getTimestamp("created_at"));
        taxpayer.setUpdatedAt(rs.getTimestamp("updated_at"));
        taxpayer.setDeleted(rs.getBoolean("deleted"));
        taxpayer.setDeclarationCount(rs.getLong("declaration_count"));
        return taxpayer;
    }
}
