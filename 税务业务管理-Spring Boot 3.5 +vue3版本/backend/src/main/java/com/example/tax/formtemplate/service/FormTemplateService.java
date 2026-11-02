package com.example.tax.formtemplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.common.enums.TemplateStatus;
import com.example.tax.declarationtype.entity.DeclarationType;
import com.example.tax.declarationtype.mapper.DeclarationTypeMapper;
import com.example.tax.formtemplate.dto.FieldOptionRequest;
import com.example.tax.formtemplate.dto.FieldRequest;
import com.example.tax.formtemplate.dto.TemplateRequest;
import com.example.tax.formtemplate.entity.FormField;
import com.example.tax.formtemplate.entity.FormTemplate;
import com.example.tax.formtemplate.mapper.FormFieldMapper;
import com.example.tax.formtemplate.mapper.FormTemplateMapper;
import com.example.tax.formtemplate.vo.FieldOptionVO;
import com.example.tax.formtemplate.vo.FieldVO;
import com.example.tax.formtemplate.vo.TemplateVO;
import com.example.tax.security.CurrentUserService;
import com.example.tax.user.entity.SysUser;
import com.example.tax.user.mapper.SysUserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FormTemplateService {

    private static final Set<String> FIELD_TYPES = Set.of(
            "TEXT", "INTEGER", "DECIMAL", "DATE", "RADIO", "MULTI_SELECT", "BOOLEAN");

    private final FormTemplateMapper templateMapper;
    private final FormFieldMapper fieldMapper;
    private final DeclarationTypeMapper typeMapper;
    private final SysUserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public FormTemplateService(FormTemplateMapper templateMapper, FormFieldMapper fieldMapper,
                               DeclarationTypeMapper typeMapper, SysUserMapper userMapper,
                               CurrentUserService currentUserService, AuditLogService auditLogService,
                               ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.fieldMapper = fieldMapper;
        this.typeMapper = typeMapper;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public PageResult<TemplateVO> page(long page, long pageSize, String keyword, Long typeId, String status) {
        LambdaQueryWrapper<FormTemplate> wrapper = new LambdaQueryWrapper<FormTemplate>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(FormTemplate::getTemplateCode, keyword)
                        .or().like(FormTemplate::getTemplateName, keyword))
                .eq(typeId != null, FormTemplate::getDeclarationTypeId, typeId)
                .eq(status != null && !status.isBlank(), FormTemplate::getStatus, status)
                .orderByDesc(FormTemplate::getUpdatedAt);
        Page<FormTemplate> resultPage = templateMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<TemplateVO> items = resultPage.getRecords().stream()
                .map(template -> toVO(template, false)).toList();
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public TemplateVO get(Long id) {
        return toVO(requireTemplate(id), true);
    }

    public TemplateVO preview(Long id) {
        return toVO(requireTemplate(id), true);
    }

    public FormTemplate requireTemplate(Long id) {
        FormTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw BusinessException.notFound("表单模板不存在");
        }
        return template;
    }

    public FormTemplate latestPublishedForType(Long typeId) {
        return templateMapper.selectLatestPublished(typeId);
    }

    @Transactional
    public TemplateVO create(TemplateRequest request) {
        if (templateMapper.selectCount(new LambdaQueryWrapper<FormTemplate>()
                .eq(FormTemplate::getTemplateCode, request.templateCode())) > 0) {
            throw BusinessException.conflict("模板编码已存在，请使用新建版本");
        }
        requireType(request.declarationTypeId());
        FormTemplate template = new FormTemplate();
        template.setTemplateCode(request.templateCode());
        template.setTemplateName(request.templateName());
        template.setDeclarationTypeId(request.declarationTypeId());
        template.setVersionNo(1);
        template.setStatus(TemplateStatus.DRAFT.name());
        template.setCreatedBy(currentUserService.userId());
        templateMapper.insert(template);
        if (request.fields() != null) {
            replaceFields(template.getId(), request.fields());
        }
        auditLogService.record("FORM_TEMPLATE_CREATE", "FORM_TEMPLATE", String.valueOf(template.getId()),
                "SUCCESS", "创建模板 " + template.getTemplateCode() + " V1");
        return get(template.getId());
    }

    @Transactional
    public TemplateVO update(Long id, TemplateRequest request) {
        FormTemplate template = requireDraft(id);
        if (!template.getTemplateCode().equals(request.templateCode())) {
            throw BusinessException.badRequest("模板编码创建后不可修改");
        }
        if (!template.getDeclarationTypeId().equals(request.declarationTypeId())) {
            throw BusinessException.badRequest("已创建的模板不能更换申报类型，请新建版本或新建模板");
        }
        FormTemplate update = new FormTemplate();
        update.setId(id);
        update.setTemplateName(request.templateName());
        templateMapper.updateById(update);
        if (request.fields() != null) {
            replaceFields(id, request.fields());
        }
        auditLogService.record("FORM_TEMPLATE_UPDATE", "FORM_TEMPLATE", String.valueOf(id), "SUCCESS",
                "更新草稿模板 " + template.getTemplateCode());
        return get(id);
    }

    @Transactional
    public FieldVO addField(Long templateId, FieldRequest request) {
        requireDraft(templateId);
        validateField(request, null);
        FormField field = toEntity(templateId, request);
        fieldMapper.insert(field);
        auditLogService.record("FORM_FIELD_ADD", "FORM_TEMPLATE", String.valueOf(templateId), "SUCCESS",
                "新增字段 " + field.getFieldCode());
        return toFieldVO(field);
    }

    @Transactional
    public FieldVO updateField(Long templateId, Long fieldId, FieldRequest request) {
        requireDraft(templateId);
        FormField existing = requireField(templateId, fieldId);
        validateField(request, fieldId);
        FormField update = toEntity(templateId, request);
        update.setId(existing.getId());
        fieldMapper.updateById(update);
        auditLogService.record("FORM_FIELD_UPDATE", "FORM_TEMPLATE", String.valueOf(templateId), "SUCCESS",
                "更新字段 " + existing.getFieldCode());
        return toFieldVO(fieldMapper.selectById(fieldId));
    }

    @Transactional
    public void deleteField(Long templateId, Long fieldId) {
        requireDraft(templateId);
        FormField existing = requireField(templateId, fieldId);
        fieldMapper.deleteById(fieldId);
        auditLogService.record("FORM_FIELD_DELETE", "FORM_TEMPLATE", String.valueOf(templateId), "SUCCESS",
                "删除字段 " + existing.getFieldCode());
    }

    @Transactional
    public TemplateVO publish(Long id) {
        FormTemplate template = requireDraft(id);
        List<FormField> fields = listFields(id).stream()
                .filter(field -> field.getEnabled() != null && field.getEnabled() == 1)
                .toList();
        if (fields.isEmpty()) {
            throw BusinessException.badRequest("模板至少需要一个启用字段才能发布");
        }
        validatePublishable(fields);
        FormTemplate update = new FormTemplate();
        update.setId(id);
        update.setStatus(TemplateStatus.PUBLISHED.name());
        update.setPublishedAt(LocalDateTime.now());
        templateMapper.updateById(update);

        DeclarationType typeUpdate = new DeclarationType();
        typeUpdate.setId(template.getDeclarationTypeId());
        typeUpdate.setDefaultTemplateId(id);
        typeMapper.updateById(typeUpdate);

        auditLogService.record("FORM_TEMPLATE_PUBLISH", "FORM_TEMPLATE", String.valueOf(id), "SUCCESS",
                "发布模板 " + template.getTemplateCode() + " V" + template.getVersionNo());
        return get(id);
    }

    @Transactional
    public void disable(Long id) {
        FormTemplate template = requireTemplate(id);
        if (!TemplateStatus.PUBLISHED.name().equals(template.getStatus())) {
            throw BusinessException.badRequest("只有已发布模板可以停用");
        }
        FormTemplate update = new FormTemplate();
        update.setId(id);
        update.setStatus(TemplateStatus.DISABLED.name());
        templateMapper.updateById(update);

        DeclarationType type = typeMapper.selectById(template.getDeclarationTypeId());
        if (type != null && id.equals(type.getDefaultTemplateId())) {
            FormTemplate fallback = templateMapper.selectOne(new LambdaQueryWrapper<FormTemplate>()
                    .eq(FormTemplate::getDeclarationTypeId, template.getDeclarationTypeId())
                    .eq(FormTemplate::getStatus, TemplateStatus.PUBLISHED.name())
                    .ne(FormTemplate::getId, id)
                    .orderByDesc(FormTemplate::getVersionNo)
                    .last("LIMIT 1"));
            DeclarationType typeUpdate = new DeclarationType();
            typeUpdate.setId(type.getId());
            typeUpdate.setDefaultTemplateId(fallback == null ? null : fallback.getId());
            typeMapper.updateById(typeUpdate);
        }
        auditLogService.record("FORM_TEMPLATE_DISABLE", "FORM_TEMPLATE", String.valueOf(id), "SUCCESS",
                "停用模板 " + template.getTemplateCode() + " V" + template.getVersionNo());
    }

    @Transactional
    public TemplateVO newVersion(Long id) {
        FormTemplate source = requireTemplate(id);
        int nextVersion = templateMapper.selectMaxVersion(source.getTemplateCode()) + 1;
        FormTemplate template = new FormTemplate();
        template.setTemplateCode(source.getTemplateCode());
        template.setTemplateName(source.getTemplateName());
        template.setDeclarationTypeId(source.getDeclarationTypeId());
        template.setVersionNo(nextVersion);
        template.setStatus(TemplateStatus.DRAFT.name());
        template.setCreatedBy(currentUserService.userId());
        templateMapper.insert(template);

        List<FormField> copied = new ArrayList<>();
        for (FormField field : listFields(id)) {
            FormField copy = new FormField();
            copy.setTemplateId(template.getId());
            copy.setFieldCode(field.getFieldCode());
            copy.setFieldLabel(field.getFieldLabel());
            copy.setFieldType(field.getFieldType());
            copy.setRequired(field.getRequired());
            copy.setMinValue(field.getMinValue());
            copy.setMaxValue(field.getMaxValue());
            copy.setMaxLength(field.getMaxLength());
            copy.setOptionsJson(field.getOptionsJson());
            copy.setValidationJson(field.getValidationJson());
            copy.setHelpText(field.getHelpText());
            copy.setSortOrder(field.getSortOrder());
            copy.setEnabled(field.getEnabled());
            fieldMapper.insert(copy);
            copied.add(copy);
        }
        auditLogService.record("FORM_TEMPLATE_NEW_VERSION", "FORM_TEMPLATE", String.valueOf(template.getId()),
                "SUCCESS", "基于 " + source.getTemplateCode() + " 创建 V" + nextVersion);
        return get(template.getId());
    }

    public List<TemplateVO> versions(Long id) {
        FormTemplate template = requireTemplate(id);
        return templateMapper.selectList(new LambdaQueryWrapper<FormTemplate>()
                        .eq(FormTemplate::getTemplateCode, template.getTemplateCode())
                        .orderByDesc(FormTemplate::getVersionNo))
                .stream().map(item -> toVO(item, false)).toList();
    }

    public List<FormField> listFields(Long templateId) {
        return fieldMapper.selectList(new LambdaQueryWrapper<FormField>()
                .eq(FormField::getTemplateId, templateId)
                .orderByAsc(FormField::getSortOrder)
                .orderByAsc(FormField::getId));
    }

    public List<FieldVO> listFieldVOs(Long templateId) {
        return listFields(templateId).stream().map(this::toFieldVO).toList();
    }

    private FormTemplate requireDraft(Long id) {
        FormTemplate template = requireTemplate(id);
        if (!TemplateStatus.DRAFT.name().equals(template.getStatus())) {
            throw BusinessException.conflict("只有草稿模板可以编辑，已发布版本不可原位修改");
        }
        return template;
    }

    private FormField requireField(Long templateId, Long fieldId) {
        FormField field = fieldMapper.selectById(fieldId);
        if (field == null || !templateId.equals(field.getTemplateId())) {
            throw BusinessException.notFound("表单字段不存在");
        }
        return field;
    }

    private void requireType(Long typeId) {
        DeclarationType type = typeMapper.selectById(typeId);
        if (type == null) {
            throw BusinessException.notFound("申报类型不存在");
        }
    }

    private void replaceFields(Long templateId, List<FieldRequest> requests) {
        Set<String> codes = new HashSet<>();
        for (FieldRequest request : requests) {
            validateField(request, null);
            if (!codes.add(request.fieldCode().toLowerCase(Locale.ROOT))) {
                throw BusinessException.badRequest("字段编码重复：" + request.fieldCode());
            }
        }
        fieldMapper.delete(new LambdaQueryWrapper<FormField>().eq(FormField::getTemplateId, templateId));
        for (FieldRequest request : requests) {
            fieldMapper.insert(toEntity(templateId, request));
        }
    }

    private void validateField(FieldRequest request, Long selfId) {
        if (!FIELD_TYPES.contains(request.fieldType())) {
            throw BusinessException.badRequest("不支持的字段类型：" + request.fieldType());
        }
        if (request.minValue() != null && request.maxValue() != null
                && request.minValue().compareTo(request.maxValue()) > 0) {
            throw BusinessException.badRequest("字段最小值不能大于最大值");
        }
        if (("RADIO".equals(request.fieldType()) || "MULTI_SELECT".equals(request.fieldType()))) {
            if (request.options() == null || request.options().isEmpty()) {
                throw BusinessException.badRequest("单选/多选字段必须配置选项");
            }
            Set<String> values = new HashSet<>();
            for (FieldOptionRequest option : request.options()) {
                if (!values.add(option.value())) {
                    throw BusinessException.badRequest("选项值重复：" + option.value());
                }
            }
        }
    }

    private void validatePublishable(List<FormField> fields) {
        Set<String> codes = new HashSet<>();
        for (FormField field : fields) {
            if (field.getFieldLabel() == null || field.getFieldLabel().isBlank()) {
                throw BusinessException.badRequest("字段 " + field.getFieldCode() + " 缺少标签");
            }
            if (!codes.add(field.getFieldCode().toLowerCase(Locale.ROOT))) {
                throw BusinessException.badRequest("字段编码重复：" + field.getFieldCode());
            }
            if (!FIELD_TYPES.contains(field.getFieldType())) {
                throw BusinessException.badRequest("字段 " + field.getFieldCode() + " 类型不合法");
            }
            if (("RADIO".equals(field.getFieldType()) || "MULTI_SELECT".equals(field.getFieldType()))
                    && (field.getOptionsJson() == null || parseOptions(field.getOptionsJson()).isEmpty())) {
                throw BusinessException.badRequest("字段 " + field.getFieldCode() + " 缺少选项配置");
            }
            if (field.getMinValue() != null && field.getMaxValue() != null
                    && field.getMinValue().compareTo(field.getMaxValue()) > 0) {
                throw BusinessException.badRequest("字段 " + field.getFieldCode() + " 最小值大于最大值");
            }
        }
    }

    private FormField toEntity(Long templateId, FieldRequest request) {
        FormField field = new FormField();
        field.setTemplateId(templateId);
        field.setFieldCode(request.fieldCode());
        field.setFieldLabel(request.fieldLabel());
        field.setFieldType(request.fieldType());
        field.setRequired(Boolean.TRUE.equals(request.required()) ? 1 : 0);
        field.setMinValue(request.minValue());
        field.setMaxValue(request.maxValue());
        field.setMaxLength(request.maxLength());
        field.setOptionsJson(writeOptions(request.options()));
        field.setHelpText(request.helpText());
        field.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        field.setEnabled(request.enabled() == null || request.enabled() ? 1 : 0);
        return field;
    }

    private String writeOptions(List<FieldOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception ex) {
            throw BusinessException.badRequest("选项配置无法序列化");
        }
    }

    private List<FieldOptionVO> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<FieldOptionRequest> options = objectMapper.readValue(json, new TypeReference<>() {
            });
            return options.stream().map(option -> new FieldOptionVO(option.label(), option.value())).toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private FieldVO toFieldVO(FormField field) {
        return new FieldVO(field.getId(), field.getTemplateId(), field.getFieldCode(), field.getFieldLabel(),
                field.getFieldType(), field.getRequired() != null && field.getRequired() == 1,
                field.getMinValue(), field.getMaxValue(), field.getMaxLength(),
                parseOptions(field.getOptionsJson()), field.getHelpText(), field.getSortOrder(),
                field.getEnabled() != null && field.getEnabled() == 1);
    }

    private TemplateVO toVO(FormTemplate template, boolean withFields) {
        DeclarationType type = typeMapper.selectById(template.getDeclarationTypeId());
        SysUser creator = userMapper.selectById(template.getCreatedBy());
        List<FieldVO> fields = withFields ? listFieldVOs(template.getId()) : List.of();
        return new TemplateVO(template.getId(), template.getTemplateCode(), template.getTemplateName(),
                template.getDeclarationTypeId(), type == null ? null : type.getTypeName(),
                template.getVersionNo(), template.getStatus(), template.getCreatedBy(),
                creator == null ? null : creator.getDisplayName(), template.getPublishedAt(),
                template.getCreatedAt(), template.getUpdatedAt(), fields);
    }
}
