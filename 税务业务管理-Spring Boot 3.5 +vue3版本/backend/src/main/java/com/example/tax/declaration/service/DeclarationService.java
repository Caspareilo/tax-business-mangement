package com.example.tax.declaration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.common.enums.DeclarationStatus;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.declaration.dto.DeclarationQuery;
import com.example.tax.declaration.dto.DeclarationSaveRequest;
import com.example.tax.declaration.entity.TaxDeclaration;
import com.example.tax.declaration.mapper.DeclarationListRow;
import com.example.tax.declaration.mapper.DeclarationMapper;
import com.example.tax.declaration.vo.DeclarationVO;
import com.example.tax.declarationtype.entity.DeclarationType;
import com.example.tax.declarationtype.mapper.DeclarationTypeMapper;
import com.example.tax.formtemplate.entity.FormTemplate;
import com.example.tax.formtemplate.service.FormTemplateService;
import com.example.tax.formtemplate.vo.FieldVO;
import com.example.tax.review.mapper.ReviewMapper;
import com.example.tax.review.vo.ReviewVO;
import com.example.tax.security.CurrentUserService;
import com.example.tax.taxpayer.entity.Taxpayer;
import com.example.tax.taxpayer.mapper.TaxpayerMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DeclarationService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999999.99");

    private final DeclarationMapper declarationMapper;
    private final TaxpayerMapper taxpayerMapper;
    private final DeclarationTypeMapper typeMapper;
    private final FormTemplateService templateService;
    private final FormDataValidator formDataValidator;
    private final ReviewMapper reviewMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final com.example.tax.user.mapper.SysUserMapper userMapper;

    public DeclarationService(DeclarationMapper declarationMapper, TaxpayerMapper taxpayerMapper,
                              DeclarationTypeMapper typeMapper, FormTemplateService templateService,
                              FormDataValidator formDataValidator, ReviewMapper reviewMapper,
                              CurrentUserService currentUserService, AuditLogService auditLogService,
                              ObjectMapper objectMapper,
                              com.example.tax.user.mapper.SysUserMapper userMapper) {
        this.userMapper = userMapper;
        this.declarationMapper = declarationMapper;
        this.taxpayerMapper = taxpayerMapper;
        this.typeMapper = typeMapper;
        this.templateService = templateService;
        this.formDataValidator = formDataValidator;
        this.reviewMapper = reviewMapper;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public PageResult<DeclarationVO> page(DeclarationQuery query, long page, long pageSize) {
        long current = Math.max(page, 1);
        long size = Math.min(Math.max(pageSize, 1), 100);
        query.setScopeUserId(currentUserService.declarationScopeUserId());
        query.setSortBy(normalizeSortBy(query.getSortBy()));
        query.setSortOrder("asc".equalsIgnoreCase(query.getSortOrder()) ? "asc" : "desc");
        IPage<DeclarationListRow> resultPage = declarationMapper.selectDeclarationPage(
                new Page<>(current, size), query);
        List<DeclarationVO> items = resultPage.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public PageResult<DeclarationVO> pageByTaxpayer(Long taxpayerId, long page, long pageSize) {
        DeclarationQuery query = new DeclarationQuery();
        query.setScopeUserId(currentUserService.declarationScopeUserId());
        query.setTaxpayerName(null);
        long current = Math.max(page, 1);
        long size = Math.min(Math.max(pageSize, 1), 100);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TaxDeclaration> p = new Page<>(current, size);
        var wrapper = new LambdaQueryWrapper<TaxDeclaration>()
                .eq(TaxDeclaration::getTaxpayerId, taxpayerId)
                .orderByDesc(TaxDeclaration::getUpdatedAt);
        Long scope = query.getScopeUserId();
        if (scope != null) {
            wrapper.eq(TaxDeclaration::getCreatedBy, scope);
        }
        var resultPage = declarationMapper.selectPage(p, wrapper);
        List<DeclarationVO> items = resultPage.getRecords().stream().map(entity -> {
            DeclarationListRow row = new DeclarationListRow();
            row.setId(entity.getId());
            row.setDeclarationNo(entity.getDeclarationNo());
            row.setTaxpayerId(entity.getTaxpayerId());
            row.setTaxpayerName(entity.getTaxpayerNameSnapshot());
            row.setDeclarationTypeId(entity.getDeclarationTypeId());
            row.setDeclarationTypeName(entity.getDeclarationTypeSnapshot());
            row.setTemplateId(entity.getTemplateId());
            row.setTemplateVersionNo(entity.getTemplateVersionNo());
            row.setPeriodStart(entity.getPeriodStart());
            row.setPeriodEnd(entity.getPeriodEnd());
            row.setStatus(entity.getStatus());
            row.setDeclaredAmount(entity.getDeclaredAmount());
            row.setCreatedBy(entity.getCreatedBy());
            row.setSubmittedAt(entity.getSubmittedAt());
            row.setLastReviewedAt(entity.getLastReviewedAt());
            row.setCreatedAt(entity.getCreatedAt());
            row.setUpdatedAt(entity.getUpdatedAt());
            return toVO(row);
        }).toList();
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public DeclarationVO get(Long id) {
        TaxDeclaration declaration = requireDeclaration(id);
        assertCanAccess(declaration);
        FormTemplate template = templateService.requireTemplate(declaration.getTemplateId());
        List<FieldVO> fields = templateService.listFieldVOs(template.getId());
        List<ReviewVO> reviews = reviewMapper.selectByDeclaration(id);
        return toDetailVO(declaration, template, fields, reviews);
    }

    public TaxDeclaration requireDeclaration(Long id) {
        TaxDeclaration declaration = declarationMapper.selectById(id);
        if (declaration == null) {
            throw BusinessException.notFound("申报记录不存在");
        }
        return declaration;
    }

    @Transactional
    public DeclarationVO create(DeclarationSaveRequest request) {
        Taxpayer taxpayer = requireActiveTaxpayer(request.taxpayerId());
        DeclarationType type = requireEnabledType(request.declarationTypeId());
        FormTemplate template = resolveTemplate(type.getId());
        validatePeriod(request.periodStart(), request.periodEnd());
        Map<String, Object> formData = formDataValidator.validate(template.getId(), request.formData());
        BigDecimal amount = normalizeAmount(request.declaredAmount());

        ensureNoDuplicate(request.taxpayerId(), request.declarationTypeId(),
                request.periodStart(), request.periodEnd(), 0L);

        TaxDeclaration declaration = new TaxDeclaration();
        declaration.setDeclarationNo(generateDeclarationNo());
        declaration.setTaxpayerId(taxpayer.getId());
        declaration.setTaxpayerNameSnapshot(taxpayer.getTaxpayerName());
        declaration.setDeclarationTypeId(type.getId());
        declaration.setDeclarationTypeSnapshot(type.getTypeName());
        declaration.setTemplateId(template.getId());
        declaration.setTemplateVersionNo(template.getVersionNo());
        declaration.setPeriodStart(request.periodStart());
        declaration.setPeriodEnd(request.periodEnd());
        declaration.setStatus(DeclarationStatus.DRAFT.name());
        declaration.setDeclaredAmount(amount);
        declaration.setFormData(writeFormData(formData));
        declaration.setCreatedBy(currentUserService.userId());
        declarationMapper.insert(declaration);
        auditLogService.record("DECLARATION_CREATE", "DECLARATION", String.valueOf(declaration.getId()),
                "SUCCESS", "创建申报草稿 " + declaration.getDeclarationNo());
        return get(declaration.getId());
    }

    @Transactional
    public DeclarationVO update(Long id, DeclarationSaveRequest request) {
        TaxDeclaration existing = requireDeclaration(id);
        assertOwnerOrAdmin(existing);
        if (!DeclarationStatus.DRAFT.name().equals(existing.getStatus())
                && !DeclarationStatus.RETURNED.name().equals(existing.getStatus())) {
            throw BusinessException.conflict("当前状态不允许修改，只有草稿或已退回的申报可编辑");
        }
        Taxpayer taxpayer = requireActiveTaxpayer(request.taxpayerId());
        DeclarationType type = requireEnabledType(request.declarationTypeId());
        validatePeriod(request.periodStart(), request.periodEnd());

        FormTemplate template;
        if (DeclarationStatus.DRAFT.name().equals(existing.getStatus())
                && (!type.getId().equals(existing.getDeclarationTypeId()))) {
            template = resolveTemplate(type.getId());
        } else {
            template = templateService.requireTemplate(existing.getTemplateId());
        }
        Map<String, Object> formData = formDataValidator.validate(template.getId(), request.formData());
        BigDecimal amount = normalizeAmount(request.declaredAmount());

        ensureNoDuplicate(request.taxpayerId(), request.declarationTypeId(),
                request.periodStart(), request.periodEnd(), id);

        TaxDeclaration update = new TaxDeclaration();
        update.setId(id);
        update.setTaxpayerId(taxpayer.getId());
        update.setTaxpayerNameSnapshot(taxpayer.getTaxpayerName());
        update.setDeclarationTypeId(type.getId());
        update.setDeclarationTypeSnapshot(type.getTypeName());
        update.setTemplateId(template.getId());
        update.setTemplateVersionNo(template.getVersionNo());
        update.setPeriodStart(request.periodStart());
        update.setPeriodEnd(request.periodEnd());
        update.setDeclaredAmount(amount);
        update.setFormData(writeFormData(formData));
        update.setVersion(existing.getVersion());
        int rows = declarationMapper.updateById(update);
        if (rows == 0) {
            throw BusinessException.conflict("申报已被其他操作修改，请刷新后重试");
        }
        auditLogService.record("DECLARATION_UPDATE", "DECLARATION", String.valueOf(id), "SUCCESS",
                "更新申报 " + existing.getDeclarationNo());
        return get(id);
    }

    @Transactional
    public void submit(Long id) {
        TaxDeclaration declaration = declarationMapper.selectFreshById(id);
        if (declaration == null) {
            throw BusinessException.notFound("申报记录不存在");
        }
        assertOwnerOrAdmin(declaration);
        if (!DeclarationStatus.DRAFT.name().equals(declaration.getStatus())
                && !DeclarationStatus.RETURNED.name().equals(declaration.getStatus())) {
            throw BusinessException.conflict("只有草稿或已退回的申报可以提交");
        }
        FormTemplate template = templateService.requireTemplate(declaration.getTemplateId());
        Map<String, Object> formData = mapFormData(declaration);
        formDataValidator.validate(template.getId(), formData);
        ensureNoDuplicate(declaration.getTaxpayerId(), declaration.getDeclarationTypeId(),
                declaration.getPeriodStart(), declaration.getPeriodEnd(), declaration.getId());

        int rows = declarationMapper.updateStatusConditional(id, declaration.getStatus(),
                DeclarationStatus.PENDING_REVIEW.name(), LocalDateTime.now());
        if (rows == 0) {
            throw BusinessException.conflict("申报状态已变化，无法重复提交");
        }
        auditLogService.record("DECLARATION_SUBMIT", "DECLARATION", String.valueOf(id), "SUCCESS",
                "提交申报 " + declaration.getDeclarationNo());
    }

    @Transactional
    public void withdraw(Long id) {
        TaxDeclaration declaration = requireDeclaration(id);
        assertOwnerOrAdmin(declaration);
        if (!DeclarationStatus.PENDING_REVIEW.name().equals(declaration.getStatus())) {
            throw BusinessException.conflict("只有待审核的申报可以撤回");
        }
        int rows = declarationMapper.updateStatusConditional(id,
                DeclarationStatus.PENDING_REVIEW.name(), DeclarationStatus.WITHDRAWN.name(), null);
        if (rows == 0) {
            throw BusinessException.conflict("申报已被审核处理，无法撤回");
        }
        auditLogService.record("DECLARATION_WITHDRAW", "DECLARATION", String.valueOf(id), "SUCCESS",
                "撤回申报 " + declaration.getDeclarationNo());
    }

    public void assertCanAccess(TaxDeclaration declaration) {
        Long scope = currentUserService.declarationScopeUserId();
        if (scope != null && !scope.equals(declaration.getCreatedBy())) {
            throw BusinessException.forbidden("没有访问该申报的权限");
        }
    }

    public void assertOwnerOrAdmin(TaxDeclaration declaration) {
        if (!currentUserService.hasRole("ADMIN")
                && !declaration.getCreatedBy().equals(currentUserService.userId())) {
            throw BusinessException.forbidden("只能操作本人创建的申报");
        }
    }

    public void ensureNoDuplicate(Long taxpayerId, Long typeId, java.time.LocalDate start,
                                  java.time.LocalDate end, Long excludeId) {
        long duplicates = declarationMapper.countEffectiveDuplicates(taxpayerId, typeId, start, end, excludeId);
        if (duplicates > 0) {
            throw BusinessException.conflict("该纳税人同一申报类型同一期间已存在有效申报，请勿重复申报");
        }
    }

    public Map<String, Object> mapFormData(TaxDeclaration declaration) {
        try {
            return objectMapper.readValue(declaration.getFormData(), new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            throw BusinessException.badRequest("申报表单数据无法解析");
        }
    }

    public String writeFormData(Map<String, Object> formData) {
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (Exception ex) {
            throw BusinessException.badRequest("申报表单数据无法保存");
        }
    }

    private Taxpayer requireActiveTaxpayer(Long taxpayerId) {
        Taxpayer taxpayer = taxpayerMapper.selectById(taxpayerId);
        if (taxpayer == null) {
            throw BusinessException.notFound("纳税人不存在");
        }
        if (!"ACTIVE".equals(taxpayer.getRegistrationStatus())) {
            throw BusinessException.badRequest("该纳税人已停用，不能新建申报");
        }
        return taxpayer;
    }

    private DeclarationType requireEnabledType(Long typeId) {
        DeclarationType type = typeMapper.selectById(typeId);
        if (type == null) {
            throw BusinessException.notFound("申报类型不存在");
        }
        if (type.getEnabled() == null || type.getEnabled() != 1) {
            throw BusinessException.badRequest("该申报类型已停用，不能用于新建申报");
        }
        return type;
    }

    private FormTemplate resolveTemplate(Long typeId) {
        FormTemplate template = templateService.latestPublishedForType(typeId);
        if (template == null) {
            throw BusinessException.conflict("该申报类型尚未发布表单模板，无法创建申报");
        }
        return template;
    }

    private void validatePeriod(java.time.LocalDate start, java.time.LocalDate end) {
        if (start.isAfter(end)) {
            throw BusinessException.badRequest("申报期间开始日期不能晚于结束日期");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.scale() > 2) {
            throw BusinessException.badRequest("申报金额最多支持两位小数");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw BusinessException.badRequest("申报金额超出允许范围");
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String generateDeclarationNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "DEC-" + datePart + "-"
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            long exists = declarationMapper.selectCount(new LambdaQueryWrapper<TaxDeclaration>()
                    .eq(TaxDeclaration::getDeclarationNo, candidate));
            if (exists == 0) {
                return candidate;
            }
        }
        throw new BusinessException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "申报编号生成失败，请重试");
    }

    private DeclarationVO toVO(DeclarationListRow row) {
        return new DeclarationVO(row.getId(), row.getDeclarationNo(), row.getTaxpayerId(),
                row.getTaxpayerCode(), row.getTaxpayerName(), row.getDeclarationTypeId(),
                row.getDeclarationTypeName(), row.getTemplateId(), null, row.getTemplateVersionNo(),
                row.getPeriodStart(), row.getPeriodEnd(), row.getStatus(), row.getDeclaredAmount(),
                null, row.getCreatedBy(), row.getCreatedByName(), row.getSubmittedAt(),
                row.getLastReviewedAt(), row.getCreatedAt(), row.getUpdatedAt(), List.of(), List.of());
    }

    private DeclarationVO toDetailVO(TaxDeclaration declaration, FormTemplate template,
                                     List<FieldVO> fields, List<ReviewVO> reviews) {
        Taxpayer taxpayer = taxpayerMapper.selectById(declaration.getTaxpayerId());
        var creator = requireDeclarationCreator(declaration.getCreatedBy());
        return new DeclarationVO(declaration.getId(), declaration.getDeclarationNo(),
                declaration.getTaxpayerId(), taxpayer == null ? null : taxpayer.getTaxpayerCode(),
                declaration.getTaxpayerNameSnapshot(), declaration.getDeclarationTypeId(),
                declaration.getDeclarationTypeSnapshot(), declaration.getTemplateId(),
                template == null ? null : template.getTemplateName(), declaration.getTemplateVersionNo(),
                declaration.getPeriodStart(), declaration.getPeriodEnd(), declaration.getStatus(),
                declaration.getDeclaredAmount(), mapFormData(declaration), declaration.getCreatedBy(),
                creator, declaration.getSubmittedAt(), declaration.getLastReviewedAt(),
                declaration.getCreatedAt(), declaration.getUpdatedAt(), fields, reviews);
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return "updatedAt";
        }
        return switch (sortBy) {
            case "submittedAt", "declaredAmount", "declarationNo", "createdAt", "updatedAt" -> sortBy;
            default -> "updatedAt";
        };
    }

    private String requireDeclarationCreator(Long userId) {
        var user = userMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }
}
