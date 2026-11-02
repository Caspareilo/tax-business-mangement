package com.example.tax.dao;

import com.example.tax.model.FormField;
import com.example.tax.model.FormTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FormTemplateDao {

    private static final String SELECT_BASE =
            "SELECT ft.*, dt.type_name, u.display_name AS created_by_name "
                    + "FROM form_template ft "
                    + "JOIN declaration_type dt ON dt.id = ft.declaration_type_id "
                    + "LEFT JOIN sys_user u ON u.id = ft.created_by ";

    public List<FormTemplate> search(Connection connection, Long typeId, String status,
                                     String keyword, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE + " WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, typeId, status, keyword);
        sql.append(" ORDER BY ft.template_code, ft.version_no DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return Jdbc.query(connection, sql.toString(), this::map, params.toArray());
    }

    public long count(Connection connection, Long typeId, String status, String keyword)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM form_template ft WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (typeId != null) {
            sql.append(" AND ft.declaration_type_id = ?");
            params.add(typeId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND ft.status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (ft.template_code LIKE ? OR ft.template_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        return Jdbc.queryCount(connection, sql.toString(), params.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> params, Long typeId,
                               String status, String keyword) {
        if (typeId != null) {
            sql.append(" AND ft.declaration_type_id = ?");
            params.add(typeId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND ft.status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (ft.template_code LIKE ? OR ft.template_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
    }

    public FormTemplate findById(Connection connection, long id) throws SQLException {
        FormTemplate template = Jdbc.queryOne(connection, SELECT_BASE + " WHERE ft.id = ?",
                this::map, id);
        if (template != null) {
            template.setFields(listFields(connection, id));
        }
        return template;
    }

    public FormTemplate findPublishedByTypeId(Connection connection, long typeId)
            throws SQLException {
        FormTemplate template = Jdbc.queryOne(connection,
                SELECT_BASE + " WHERE ft.declaration_type_id = ? AND ft.status = 'PUBLISHED' "
                        + "ORDER BY ft.version_no DESC LIMIT 1",
                this::map, typeId);
        if (template != null) {
            template.setFields(listFields(connection, template.getId()));
        }
        return template;
    }

    public FormTemplate findLatestByCode(Connection connection, String templateCode)
            throws SQLException {
        return Jdbc.queryOne(connection,
                SELECT_BASE + " WHERE ft.template_code = ? ORDER BY ft.version_no DESC LIMIT 1",
                this::map, templateCode);
    }

    public List<FormTemplate> listVersions(Connection connection, String templateCode)
            throws SQLException {
        List<FormTemplate> versions = Jdbc.query(connection,
                SELECT_BASE + " WHERE ft.template_code = ? ORDER BY ft.version_no DESC",
                this::map, templateCode);
        for (FormTemplate version : versions) {
            version.setFields(listFields(connection, version.getId()));
        }
        return versions;
    }

    public List<FormField> listFields(Connection connection, long templateId) throws SQLException {
        return Jdbc.query(connection,
                "SELECT * FROM form_field WHERE template_id = ? ORDER BY sort_order, id",
                this::mapField, templateId);
    }

    public int nextVersion(Connection connection, String templateCode) throws SQLException {
        long max = Jdbc.queryCount(connection,
                "SELECT COALESCE(MAX(version_no), 0) FROM form_template WHERE template_code = ?",
                templateCode);
        return (int) max + 1;
    }

    public long insert(Connection connection, FormTemplate template) throws SQLException {
        return Jdbc.insert(connection,
                "INSERT INTO form_template (template_code, template_name, declaration_type_id, "
                        + "version_no, status, created_by, published_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                template.getTemplateCode(), template.getTemplateName(),
                template.getDeclarationTypeId(), template.getVersionNo(), template.getStatus(),
                template.getCreatedBy());
    }

    public int updateDraftMeta(Connection connection, long id, String templateName)
            throws SQLException {
        return Jdbc.update(connection,
                "UPDATE form_template SET template_name = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND status = 'DRAFT'", templateName, id);
    }

    public void replaceFields(Connection connection, long templateId, List<FormField> fields)
            throws SQLException {
        Jdbc.update(connection, "DELETE FROM form_field WHERE template_id = ?", templateId);
        insertFields(connection, templateId, fields);
    }

    public void insertFields(Connection connection, long templateId, List<FormField> fields)
            throws SQLException {
        for (FormField field : fields) {
            Jdbc.insert(connection,
                    "INSERT INTO form_field (template_id, field_code, field_label, field_type, "
                            + "required, min_value, max_value, max_length, options_json, help_text, "
                            + "sort_order, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    templateId, field.getFieldCode(), field.getFieldLabel(), field.getFieldType(),
                    field.isRequired(), field.getMinValue(), field.getMaxValue(),
                    field.getMaxLength(), field.getOptionsJson(), field.getHelpText(),
                    field.getSortOrder(), field.isEnabled());
        }
    }

    public int publish(Connection connection, long id) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE form_template SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP, "
                        + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'DRAFT'", id);
    }

    public int disable(Connection connection, long id) throws SQLException {
        return Jdbc.update(connection,
                "UPDATE form_template SET status = 'DISABLED', updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND status = 'PUBLISHED'", id);
    }

    /** Publishing a version makes it the default for its type; older published ones are retired. */
    public int disableOtherPublished(Connection connection, long typeId, long exceptId)
            throws SQLException {
        return Jdbc.update(connection,
                "UPDATE form_template SET status = 'DISABLED', updated_at = CURRENT_TIMESTAMP "
                        + "WHERE declaration_type_id = ? AND status = 'PUBLISHED' AND id <> ?",
                typeId, exceptId);
    }

    public long countByTypeAndStatus(Connection connection, long typeId, String status)
            throws SQLException {
        return Jdbc.queryCount(connection,
                "SELECT COUNT(*) FROM form_template WHERE declaration_type_id = ? AND status = ?",
                typeId, status);
    }

    private FormTemplate map(ResultSet rs) throws SQLException {
        FormTemplate template = new FormTemplate();
        template.setId(rs.getLong("id"));
        template.setTemplateCode(rs.getString("template_code"));
        template.setTemplateName(rs.getString("template_name"));
        template.setDeclarationTypeId(rs.getLong("declaration_type_id"));
        template.setVersionNo(rs.getInt("version_no"));
        template.setStatus(rs.getString("status"));
        template.setCreatedBy(rs.getLong("created_by"));
        template.setPublishedAt(rs.getTimestamp("published_at"));
        template.setCreatedAt(rs.getTimestamp("created_at"));
        template.setUpdatedAt(rs.getTimestamp("updated_at"));
        template.setDeclarationTypeName(rs.getString("type_name"));
        template.setCreatedByName(rs.getString("created_by_name"));
        return template;
    }

    private FormField mapField(ResultSet rs) throws SQLException {
        FormField field = new FormField();
        field.setId(rs.getLong("id"));
        field.setTemplateId(rs.getLong("template_id"));
        field.setFieldCode(rs.getString("field_code"));
        field.setFieldLabel(rs.getString("field_label"));
        field.setFieldType(rs.getString("field_type"));
        field.setRequired(rs.getBoolean("required"));
        field.setMinValue(rs.getBigDecimal("min_value"));
        field.setMaxValue(rs.getBigDecimal("max_value"));
        int maxLength = rs.getInt("max_length");
        field.setMaxLength(rs.wasNull() ? null : maxLength);
        field.setOptionsJson(rs.getString("options_json"));
        field.setHelpText(rs.getString("help_text"));
        field.setSortOrder(rs.getInt("sort_order"));
        field.setEnabled(rs.getBoolean("enabled"));
        return field;
    }
}
