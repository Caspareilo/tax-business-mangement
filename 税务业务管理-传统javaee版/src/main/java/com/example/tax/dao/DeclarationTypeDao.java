package com.example.tax.dao;

import com.example.tax.model.DeclarationType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeclarationTypeDao {

    public List<DeclarationType> search(Connection connection, String keyword, String enabled,
                                        int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT dt.*, (SELECT COUNT(*) FROM form_template ft "
                        + "WHERE ft.declaration_type_id = dt.id AND ft.status = 'PUBLISHED') "
                        + "AS published_template_count FROM declaration_type dt WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, keyword, enabled);
        sql.append(" ORDER BY dt.id LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return Jdbc.query(connection, sql.toString(), this::map, params.toArray());
    }

    public long count(Connection connection, String keyword, String enabled) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM declaration_type dt WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, keyword, enabled);
        return Jdbc.queryCount(connection, sql.toString(), params.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String keyword,
                               String enabled) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (dt.type_code LIKE ? OR dt.type_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if ("1".equals(enabled)) {
            sql.append(" AND dt.enabled = 1");
        } else if ("0".equals(enabled)) {
            sql.append(" AND dt.enabled = 0");
        }
    }

    public List<DeclarationType> listEnabled(Connection connection) throws SQLException {
        return Jdbc.query(connection,
                "SELECT dt.*, (SELECT COUNT(*) FROM form_template ft "
                        + "WHERE ft.declaration_type_id = dt.id AND ft.status = 'PUBLISHED') "
                        + "AS published_template_count FROM declaration_type dt "
                        + "WHERE dt.enabled = 1 ORDER BY dt.id", this::map);
    }

    public List<DeclarationType> listAll(Connection connection) throws SQLException {
        return Jdbc.query(connection,
                "SELECT dt.*, (SELECT COUNT(*) FROM form_template ft "
                        + "WHERE ft.declaration_type_id = dt.id AND ft.status = 'PUBLISHED') "
                        + "AS published_template_count FROM declaration_type dt ORDER BY dt.id",
                this::map);
    }

    public DeclarationType findById(Connection connection, long id) throws SQLException {
        return Jdbc.queryOne(connection,
                "SELECT dt.*, (SELECT COUNT(*) FROM form_template ft "
                        + "WHERE ft.declaration_type_id = dt.id AND ft.status = 'PUBLISHED') "
                        + "AS published_template_count FROM declaration_type dt WHERE dt.id = ?",
                this::map, id);
    }

    public DeclarationType findByCode(Connection connection, String code) throws SQLException {
        return Jdbc.queryOne(connection,
                "SELECT dt.*, 0 AS published_template_count FROM declaration_type dt "
                        + "WHERE dt.type_code = ?", this::map, code);
    }

    public long insert(Connection connection, DeclarationType type) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO declaration_type (type_code, type_name, description, enabled, "
                        + "deadline_day, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                type.getTypeCode(), type.getTypeName(), type.getDescription(),
                type.isEnabled(), type.getDeadlineDay());
    }

    public int update(Connection connection, DeclarationType type) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE declaration_type SET type_name = ?, description = ?, deadline_day = ?, "
                        + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND type_code = ?",
                type.getTypeName(), type.getDescription(), type.getDeadlineDay(),
                type.getId(), type.getTypeCode());
    }

    public int setEnabled(Connection connection, long id, boolean enabled) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE declaration_type SET enabled = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ?", enabled, id);
    }

    public long countPublishedTemplates(Connection connection, long typeId) throws SQLException {
        return Jdbc.queryCount(connection,
                "SELECT COUNT(*) FROM form_template WHERE declaration_type_id = ? "
                        + "AND status = 'PUBLISHED'", typeId);
    }

    private DeclarationType map(ResultSet rs) throws SQLException {
        DeclarationType type = new DeclarationType();
        type.setId(rs.getLong("id"));
        type.setTypeCode(rs.getString("type_code"));
        type.setTypeName(rs.getString("type_name"));
        type.setDescription(rs.getString("description"));
        type.setEnabled(rs.getBoolean("enabled"));
        int deadlineDay = rs.getInt("deadline_day");
        type.setDeadlineDay(rs.wasNull() ? null : deadlineDay);
        type.setCreatedAt(rs.getTimestamp("created_at"));
        type.setUpdatedAt(rs.getTimestamp("updated_at"));
        type.setPublishedTemplateCount(rs.getInt("published_template_count"));
        return type;
    }
}
