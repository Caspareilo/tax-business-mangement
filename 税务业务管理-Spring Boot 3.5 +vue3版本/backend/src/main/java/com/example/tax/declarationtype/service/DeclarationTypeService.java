package com.example.tax.declarationtype.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.declarationtype.dto.DeclarationTypeRequest;
import com.example.tax.declarationtype.entity.DeclarationType;
import com.example.tax.declarationtype.mapper.DeclarationTypeMapper;
import com.example.tax.declarationtype.vo.DeclarationTypeVO;
import com.example.tax.formtemplate.entity.FormTemplate;
import com.example.tax.formtemplate.mapper.FormTemplateMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeclarationTypeService {

    private final DeclarationTypeMapper typeMapper;
    private final FormTemplateMapper templateMapper;
    private final AuditLogService auditLogService;

    public DeclarationTypeService(DeclarationTypeMapper typeMapper, FormTemplateMapper templateMapper,
                                  AuditLogService auditLogService) {
        this.typeMapper = typeMapper;
        this.templateMapper = templateMapper;
        this.auditLogService = auditLogService;
    }

    public PageResult<DeclarationTypeVO> page(long page, long pageSize, String keyword, Boolean enabled) {
        LambdaQueryWrapper<DeclarationType> wrapper = new LambdaQueryWrapper<DeclarationType>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(DeclarationType::getTypeCode, keyword)
                        .or().like(DeclarationType::getTypeName, keyword))
                .eq(enabled != null, DeclarationType::getEnabled, enabled == null ? null : (enabled ? 1 : 0))
                .orderByAsc(DeclarationType::getId);
        Page<DeclarationType> resultPage = typeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<DeclarationTypeVO> items = resultPage.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public List<DeclarationTypeVO> enabledOptions() {
        return typeMapper.selectList(new LambdaQueryWrapper<DeclarationType>()
                        .eq(DeclarationType::getEnabled, 1)
                        .orderByAsc(DeclarationType::getId))
                .stream().map(this::toVO).toList();
    }

    public DeclarationTypeVO get(Long id) {
        return toVO(requireType(id));
    }

    public DeclarationType requireType(Long id) {
        DeclarationType type = typeMapper.selectById(id);
        if (type == null) {
            throw BusinessException.notFound("申报类型不存在");
        }
        return type;
    }

    @Transactional
    public DeclarationTypeVO create(DeclarationTypeRequest request) {
        if (typeMapper.selectCount(new LambdaQueryWrapper<DeclarationType>()
                .eq(DeclarationType::getTypeCode, request.typeCode())) > 0) {
            throw BusinessException.conflict("申报类型编码已存在");
        }
        DeclarationType type = new DeclarationType();
        type.setTypeCode(request.typeCode());
        type.setTypeName(request.typeName());
        type.setDescription(request.description());
        type.setEnabled(1);
        try {
            typeMapper.insert(type);
        } catch (DuplicateKeyException ex) {
            throw BusinessException.conflict("申报类型编码已存在");
        }
        auditLogService.record("DECLARATION_TYPE_CREATE", "DECLARATION_TYPE",
                String.valueOf(type.getId()), "SUCCESS", "新增申报类型 " + type.getTypeCode());
        return toVO(type);
    }

    @Transactional
    public DeclarationTypeVO update(Long id, DeclarationTypeRequest request) {
        DeclarationType existing = requireType(id);
        if (!existing.getTypeCode().equals(request.typeCode())) {
            throw BusinessException.badRequest("申报类型编码创建后不可修改");
        }
        DeclarationType update = new DeclarationType();
        update.setId(id);
        update.setTypeName(request.typeName());
        update.setDescription(request.description());
        typeMapper.updateById(update);
        auditLogService.record("DECLARATION_TYPE_UPDATE", "DECLARATION_TYPE", String.valueOf(id),
                "SUCCESS", "更新申报类型 " + existing.getTypeCode());
        return toVO(typeMapper.selectById(id));
    }

    @Transactional
    public void updateStatus(Long id, boolean enabled) {
        DeclarationType existing = requireType(id);
        DeclarationType update = new DeclarationType();
        update.setId(id);
        update.setEnabled(enabled ? 1 : 0);
        typeMapper.updateById(update);
        auditLogService.record("DECLARATION_TYPE_STATUS", "DECLARATION_TYPE", String.valueOf(id),
                "SUCCESS", (enabled ? "启用" : "停用") + "申报类型 " + existing.getTypeCode());
    }

    private DeclarationTypeVO toVO(DeclarationType type) {
        String templateName = null;
        if (type.getDefaultTemplateId() != null) {
            FormTemplate template = templateMapper.selectById(type.getDefaultTemplateId());
            if (template != null) {
                templateName = template.getTemplateName() + " V" + template.getVersionNo();
            }
        }
        return DeclarationTypeVO.from(type, templateName);
    }
}
