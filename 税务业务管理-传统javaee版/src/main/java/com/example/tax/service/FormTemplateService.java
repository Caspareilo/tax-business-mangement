package com.example.tax.service;

import com.example.tax.dao.DeclarationTypeDao;
import com.example.tax.dao.FormTemplateDao;
import com.example.tax.dto.FieldInput;
import com.example.tax.dto.Page;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.DeclarationType;
import com.example.tax.model.FormField;
import com.example.tax.model.FormTemplate;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FormTemplateService {

    private static final Pattern FIELD_CODE = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final List<String> FIELD_TYPES = List.of("TEXT", "INTEGER", "AMOUNT",
            "DATE", "RADIO", "MULTI", "BOOLEAN");

    private final FormTemplateDao templateDao = new FormTemplateDao();
    private final DeclarationTypeDao typeDao = new DeclarationTypeDao();
    private final AuditService auditService = new AuditService();

    public Page<FormTemplate> list(Long typeId, String status, String keyword, int page, int size)
            throws SQLException {
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = templateDao.count(connection, typeId, status, keyword);
            List<FormTemplate> rows = templateDao.search(connection, typeId, status, keyword,
                    (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public FormTemplate findById(long id) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            FormTemplate template = templateDao.findById(connection, id);
            if (template == null) {
                throw new NotFoundException("表单模板不存在");
            }
            return template;
        }
    }

    public List<FormTemplate> listVersions(String templateCode) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return templateDao.listVersions(connection, templateCode);
        }
    }

    public long createDraft(SessionUser actor, long typeId, String templateCode,
                            String templateName, List<FieldInput> inputs, String traceId)
            throws SQLException {
        requireManagePermission(actor);
        List<FormField> fields = validateInputs(inputs);
        Map<String, String> errors = ValidationUtil.newErrors();
        ValidationUtil.requireCode(errors, "templateCode", "模板编码", templateCode);
        ValidationUtil.requireText(errors, "templateName", "模板名称", templateName, 150);
        ValidationUtil.throwIfAny(errors, "请检查表单模板");

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                DeclarationType type = typeDao.findById(connection, typeId);
                if (type == null) {
                    throw new ValidationException("申报类型不存在",
                            Map.of("typeId", "请选择有效申报类型"));
                }
                FormTemplate latest = templateDao.findLatestByCode(connection, templateCode.trim());
                if (latest != null) {
                    throw new ValidationException("模板编码已存在",
                            Map.of("templateCode", "该模板编码已有版本，请使用“新建版本”"));
                }
                FormTemplate template = new FormTemplate();
                template.setTemplateCode(templateCode.trim());
                template.setTemplateName(templateName.trim());
                template.setDeclarationTypeId(typeId);
                template.setVersionNo(1);
                template.setStatus("DRAFT");
                template.setCreatedBy(actor.getId());
                long id = templateDao.insert(connection, template);
                templateDao.insertFields(connection, id, fields);
                auditService.record(connection, actor, "TEMPLATE_CREATE", "FORM_TEMPLATE",
                        String.valueOf(id), true,
                        "创建表单模板 " + templateCode.trim() + " v1（草稿）", traceId);
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

    public void saveDraft(SessionUser actor, long id, String templateName,
                          List<FieldInput> inputs, String traceId) throws SQLException {
        requireManagePermission(actor);
        List<FormField> fields = validateInputs(inputs);
        Map<String, String> errors = ValidationUtil.newErrors();
        ValidationUtil.requireText(errors, "templateName", "模板名称", templateName, 150);
        ValidationUtil.throwIfAny(errors, "请检查表单模板");

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                FormTemplate template = templateDao.findById(connection, id);
                if (template == null) {
                    throw new NotFoundException("表单模板不存在");
                }
                if (!template.isDraft()) {
                    throw new BusinessException("已发布或已停用的模板不能原位修改，请创建新版本");
                }
                templateDao.updateDraftMeta(connection, id, templateName.trim());
                templateDao.replaceFields(connection, id, fields);
                auditService.record(connection, actor, "TEMPLATE_SAVE", "FORM_TEMPLATE",
                        String.valueOf(id), true,
                        "保存模板草稿 " + template.getTemplateCode() + " v"
                                + template.getVersionNo() + "，字段数 " + fields.size(), traceId);
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public long createNewVersion(SessionUser actor, long templateId, String traceId)
            throws SQLException {
        requireManagePermission(actor);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                FormTemplate source = templateDao.findById(connection, templateId);
                if (source == null) {
                    throw new NotFoundException("表单模板不存在");
                }
                FormTemplate latest = templateDao.findLatestByCode(connection, source.getTemplateCode());
                if (latest != null && "DRAFT".equals(latest.getStatus())) {
                    throw new BusinessException("该模板已存在草稿版本，请先编辑或发布现有草稿");
                }
                FormTemplate draft = new FormTemplate();
                draft.setTemplateCode(source.getTemplateCode());
                draft.setTemplateName(source.getTemplateName());
                draft.setDeclarationTypeId(source.getDeclarationTypeId());
                draft.setVersionNo(templateDao.nextVersion(connection, source.getTemplateCode()));
                draft.setStatus("DRAFT");
                draft.setCreatedBy(actor.getId());
                long id = templateDao.insert(connection, draft);
                List<FormField> copied = new ArrayList<>();
                for (FormField field : source.getFields()) {
                    FormField copy = new FormField();
                    copy.setFieldCode(field.getFieldCode());
                    copy.setFieldLabel(field.getFieldLabel());
                    copy.setFieldType(field.getFieldType());
                    copy.setRequired(field.isRequired());
                    copy.setMinValue(field.getMinValue());
                    copy.setMaxValue(field.getMaxValue());
                    copy.setMaxLength(field.getMaxLength());
                    copy.setOptionsJson(field.getOptionsJson());
                    copy.setHelpText(field.getHelpText());
                    copy.setSortOrder(field.getSortOrder());
                    copy.setEnabled(true);
                    copied.add(copy);
                }
                templateDao.insertFields(connection, id, copied);
                auditService.record(connection, actor, "TEMPLATE_VERSION", "FORM_TEMPLATE",
                        String.valueOf(id), true, "基于模板 " + source.getTemplateCode()
                                + " v" + source.getVersionNo() + " 创建 v"
                                + draft.getVersionNo() + " 草稿", traceId);
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

    public void publish(SessionUser actor, long id, String traceId) throws SQLException {
        requireManagePermission(actor);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                FormTemplate template = templateDao.findById(connection, id);
                if (template == null) {
                    throw new NotFoundException("表单模板不存在");
                }
                if (!template.isDraft()) {
                    throw new BusinessException("只有草稿模板可以发布");
                }
                if (template.getFields().isEmpty()) {
                    throw new BusinessException("模板至少需要一个字段才能发布");
                }
                templateDao.disableOtherPublished(connection, template.getDeclarationTypeId(),
                        template.getId());
                int rows = templateDao.publish(connection, id);
                if (rows == 0) {
                    throw new BusinessException("模板状态已变化，请刷新后重试");
                }
                auditService.record(connection, actor, "TEMPLATE_PUBLISH", "FORM_TEMPLATE",
                        String.valueOf(id), true, "发布模板 " + template.getTemplateCode()
                                + " v" + template.getVersionNo(), traceId);
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void disable(SessionUser actor, long id, String traceId) throws SQLException {
        requireManagePermission(actor);
        try (Connection connection = Db.getConnection()) {
            FormTemplate template = templateDao.findById(connection, id);
            if (template == null) {
                throw new NotFoundException("表单模板不存在");
            }
            int rows = templateDao.disable(connection, id);
            if (rows == 0) {
                throw new BusinessException("只有已发布的模板可以停用");
            }
            auditService.record(connection, actor, "TEMPLATE_DISABLE", "FORM_TEMPLATE",
                    String.valueOf(id), true, "停用模板 " + template.getTemplateCode()
                            + " v" + template.getVersionNo(), traceId);
        }
    }

    public List<FormField> validateInputs(List<FieldInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new ValidationException("模板至少需要一个字段",
                    Map.of("fields", "请至少添加一个字段"));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        List<FormField> fields = new ArrayList<>();
        java.util.Set<String> codes = new java.util.HashSet<>();
        int index = 0;
        for (FieldInput input : inputs) {
            String prefix = "fields[" + index + "].";
            String code = input.getFieldCode() == null ? "" : input.getFieldCode().trim();
            String label = input.getFieldLabel() == null ? "" : input.getFieldLabel().trim();
            String type = input.getFieldType() == null ? "" : input.getFieldType().trim();
            if (code.isEmpty()) {
                errors.put(prefix + "fieldCode", "字段编码不能为空");
            } else if (!FIELD_CODE.matcher(code).matches()) {
                errors.put(prefix + "fieldCode", "字段编码须以字母开头，仅含字母、数字、下划线");
            } else if (!codes.add(code.toLowerCase())) {
                errors.put(prefix + "fieldCode", "字段编码不能重复");
            }
            if (label.isEmpty()) {
                errors.put(prefix + "fieldLabel", "字段名称不能为空");
            } else if (label.length() > 150) {
                errors.put(prefix + "fieldLabel", "字段名称不能超过 150 字符");
            }
            if (!FIELD_TYPES.contains(type)) {
                errors.put(prefix + "fieldType", "字段类型不合法");
            }
            if ("RADIO".equals(type) || "MULTI".equals(type)) {
                if (input.optionList().size() < 2) {
                    errors.put(prefix + "options", "单选/多选字段至少需要 2 个选项");
                }
            }
            if ("TEXT".equals(type)) {
                String maxLength = input.getMaxLength();
                if (maxLength != null && !maxLength.trim().isEmpty()) {
                    try {
                        int value = Integer.parseInt(maxLength.trim());
                        if (value < 1 || value > 2000) {
                            errors.put(prefix + "maxLength", "文本长度须在 1-2000 之间");
                        }
                    } catch (NumberFormatException e) {
                        errors.put(prefix + "maxLength", "文本长度必须为整数");
                    }
                }
            }
            if ("INTEGER".equals(type) || "AMOUNT".equals(type)) {
                BigDecimal min = parseDecimal(input.getMinValue(), prefix + "minValue", errors);
                BigDecimal max = parseDecimal(input.getMaxValue(), prefix + "maxValue", errors);
                if (min != null && max != null && min.compareTo(max) > 0) {
                    errors.put(prefix + "maxValue", "最大值不能小于最小值");
                }
            }
            FieldInput normalized = new FieldInput();
            normalized.setFieldCode(code);
            normalized.setFieldLabel(label);
            normalized.setFieldType(type);
            normalized.setRequired(input.isRequired());
            normalized.setMinValue(input.getMinValue());
            normalized.setMaxValue(input.getMaxValue());
            normalized.setMaxLength(input.getMaxLength());
            normalized.setOptions(input.getOptions());
            normalized.setHelpText(input.getHelpText());
            fields.add(normalized.toFormField(index));
            index++;
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("表单字段配置有误", errors);
        }
        return fields;
    }

    private BigDecimal parseDecimal(String value, String key, Map<String, String> errors) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            errors.put(key, "请输入合法数字");
            return null;
        }
    }

    private void requireManagePermission(SessionUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.TEMPLATE_MANAGE)) {
            throw new ForbiddenException("无权管理表单模板");
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
