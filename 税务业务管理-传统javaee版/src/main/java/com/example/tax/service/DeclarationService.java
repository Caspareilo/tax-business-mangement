package com.example.tax.service;

import com.example.tax.dao.DeclarationDao;
import com.example.tax.dao.DeclarationTypeDao;
import com.example.tax.dao.FormTemplateDao;
import com.example.tax.dao.ReviewDao;
import com.example.tax.dao.TaxpayerDao;
import com.example.tax.dto.DeclarationFilter;
import com.example.tax.dto.DeclarationForm;
import com.example.tax.dto.Page;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.DeclarationReview;
import com.example.tax.model.DeclarationType;
import com.example.tax.model.FormField;
import com.example.tax.model.FormTemplate;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.model.Taxpayer;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.DateUtil;
import com.example.tax.util.Db;
import com.example.tax.util.JsonUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class DeclarationService {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d{1,18}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d{1,16}(\\.\\d{1,2})?$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_EXPORT_ROWS = 5000;

    private final DeclarationDao declarationDao = new DeclarationDao();
    private final TaxpayerDao taxpayerDao = new TaxpayerDao();
    private final DeclarationTypeDao typeDao = new DeclarationTypeDao();
    private final FormTemplateDao templateDao = new FormTemplateDao();
    private final ReviewDao reviewDao = new ReviewDao();
    private final AuditService auditService = new AuditService();

    public Page<TaxDeclaration> list(SessionUser actor, DeclarationFilter filter, int page,
                                     int size) throws SQLException {
        requireViewPermission(actor);
        DeclarationFilter scoped = scopeFilter(actor, filter);
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = declarationDao.count(connection, scoped);
            List<TaxDeclaration> rows = declarationDao.search(connection, scoped,
                    (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public Page<TaxDeclaration> listMine(SessionUser actor, DeclarationFilter filter, int page,
                                         int size) throws SQLException {
        requireViewPermission(actor);
        DeclarationFilter scoped = filter == null ? new DeclarationFilter() : filter;
        scoped.setCreatedBy(actor.getId());
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = declarationDao.count(connection, scoped);
            List<TaxDeclaration> rows = declarationDao.search(connection, scoped,
                    (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public TaxDeclaration findDetail(long id, SessionUser actor) throws SQLException {
        requireViewPermission(actor);
        try (Connection connection = Db.getConnection()) {
            TaxDeclaration declaration = declarationDao.findById(connection, id);
            if (declaration == null) {
                throw new NotFoundException("申报记录不存在");
            }
            if (!canViewAll(actor) && declaration.getCreatedBy() != actor.getId()) {
                throw new ForbiddenException("只能查看本人提交的申报");
            }
            List<FormField> fields = templateDao.listFields(connection, declaration.getTemplateId());
            declaration.setTemplateFields(fields);
            declaration.setFormValues(JsonUtil.parseFlatObject(declaration.getFormDataJson()));
            List<DeclarationReview> reviews = reviewDao.listByDeclaration(connection, id);
            declaration.setReviews(reviews);
            return declaration;
        }
    }

    public TaxDeclaration findForEdit(long id, SessionUser actor) throws SQLException {
        if (!actor.hasPermission(PermissionCodes.DECLARATION_CREATE)) {
            throw new ForbiddenException("无权编辑申报");
        }
        try (Connection connection = Db.getConnection()) {
            TaxDeclaration declaration = declarationDao.findById(connection, id);
            if (declaration == null) {
                throw new NotFoundException("申报记录不存在");
            }
            if (declaration.getCreatedBy() != actor.getId()) {
                throw new ForbiddenException("只能编辑本人创建的申报");
            }
            if (!declaration.isEditable()) {
                throw new BusinessException("只有草稿或已退回的申报可以编辑");
            }
            declaration.setTemplateFields(templateDao.listFields(connection,
                    declaration.getTemplateId()));
            declaration.setFormValues(JsonUtil.parseFlatObject(declaration.getFormDataJson()));
            return declaration;
        }
    }

    /** Latest published template used when starting a new declaration of the given type. */
    public FormTemplate findPublishedTemplate(long typeId) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            FormTemplate template = templateDao.findPublishedByTypeId(connection, typeId);
            if (template == null) {
                throw new BusinessException("该申报类型尚未发布可用表单模板");
            }
            return template;
        }
    }

    public long saveDraft(SessionUser actor, DeclarationForm form, String traceId)
            throws SQLException {
        if (!actor.hasPermission(PermissionCodes.DECLARATION_CREATE)) {
            throw new ForbiddenException("无权创建申报");
        }
        validateBasic(form);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Taxpayer taxpayer = taxpayerDao.findById(connection,
                        requireId(form.getTaxpayerId(), "请选择纳税人"));
                if (taxpayer == null) {
                    throw new ValidationException("纳税人不存在",
                            Map.of("taxpayerId", "请选择有效纳税人"));
                }
                if (!taxpayer.isActive()) {
                    throw new ValidationException("纳税人已停用",
                            Map.of("taxpayerId", "该纳税人已停用，不能新建申报"));
                }
                DeclarationType type = typeDao.findById(connection,
                        requireId(form.getDeclarationTypeId(), "请选择申报类型"));
                if (type == null || !type.isEnabled()) {
                    throw new ValidationException("申报类型不可用",
                            Map.of("declarationTypeId", "该申报类型不存在或已停用"));
                }

                TaxDeclaration existing = null;
                if (form.getId() != null) {
                    existing = declarationDao.findById(connection, form.getId());
                    if (existing == null) {
                        throw new NotFoundException("申报记录不存在");
                    }
                    if (existing.getCreatedBy() != actor.getId()) {
                        throw new ForbiddenException("只能编辑本人创建的申报");
                    }
                    if (!existing.isEditable()) {
                        throw new BusinessException("只有草稿或已退回的申报可以编辑");
                    }
                }

                long templateId;
                int templateVersionNo;
                if (existing != null && existing.getDeclarationTypeId() == type.getId()) {
                    templateId = existing.getTemplateId();
                    templateVersionNo = existing.getTemplateVersionNo();
                } else {
                    FormTemplate template = templateDao.findPublishedByTypeId(connection,
                            type.getId());
                    if (template == null) {
                        throw new BusinessException("该申报类型尚未发布可用表单模板");
                    }
                    templateId = template.getId();
                    templateVersionNo = template.getVersionNo();
                }

                List<FormField> fields = templateDao.listFields(connection, templateId);
                Map<String, String> values = normalizeValues(fields, form.getFormValues());

                TaxDeclaration declaration = existing == null ? new TaxDeclaration() : existing;
                declaration.setTaxpayerId(taxpayer.getId());
                declaration.setTaxpayerNameSnapshot(taxpayer.getTaxpayerName());
                declaration.setDeclarationTypeId(type.getId());
                declaration.setDeclarationTypeSnapshot(type.getTypeName());
                declaration.setTemplateId(templateId);
                declaration.setTemplateVersionNo(templateVersionNo);
                declaration.setPeriodStart(form.getPeriodStart());
                declaration.setPeriodEnd(form.getPeriodEnd());
                declaration.setDeclaredAmount(computeAmount(fields, values));
                declaration.setFormDataJson(JsonUtil.toJson(values));
                declaration.setCreatedBy(actor.getId());

                long id;
                if (existing == null) {
                    declaration.setDeclarationNo(generateDeclarationNo());
                    declaration.setStatus(TaxDeclaration.STATUS_DRAFT);
                    id = declarationDao.insert(connection, declaration);
                    auditService.record(connection, actor, "DECLARATION_SAVE", "DECLARATION",
                            String.valueOf(id), true, "保存申报草稿 " + declaration.getDeclarationNo(),
                            traceId);
                } else {
                    declaration.setStatus(existing.getStatus());
                    int rows = declarationDao.updateDraft(connection, declaration);
                    if (rows == 0) {
                        throw new BusinessException("申报状态已变化，请刷新后重试");
                    }
                    id = declaration.getId();
                    auditService.record(connection, actor, "DECLARATION_SAVE", "DECLARATION",
                            String.valueOf(id), true,
                            "更新申报草稿 " + declaration.getDeclarationNo(), traceId);
                }
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

    public void submit(SessionUser actor, long id, String traceId) throws SQLException {
        if (!actor.hasPermission(PermissionCodes.DECLARATION_SUBMIT)) {
            throw new ForbiddenException("无权限提交申报");
        }
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                TaxDeclaration declaration = declarationDao.findById(connection, id);
                if (declaration == null) {
                    throw new NotFoundException("申报记录不存在");
                }
                if (declaration.getCreatedBy() != actor.getId()) {
                    throw new ForbiddenException("只能提交本人创建的申报");
                }
                if (!declaration.isEditable()) {
                    throw new BusinessException("只有草稿或已退回的申报可以提交");
                }
                List<FormField> fields = templateDao.listFields(connection,
                        declaration.getTemplateId());
                Map<String, String> values = normalizeValues(fields,
                        JsonUtil.parseFlatObject(declaration.getFormDataJson()));
                Map<String, String> errors = validateFieldValues(fields, values);
                if (!errors.isEmpty()) {
                    throw new ValidationException("申报表单校验未通过，请补全必填项", errors);
                }
                if (declarationDao.existsActiveDuplicate(connection, declaration.getTaxpayerId(),
                        declaration.getDeclarationTypeId(), declaration.getPeriodStart(),
                        declaration.getPeriodEnd(), declaration.getId())) {
                    throw new BusinessException("该纳税人相同申报类型和期间的申报已在流程中或已通过，"
                            + "不能重复提交");
                }
                BigDecimal amount = computeAmount(fields, values);
                int rows = declarationDao.submit(connection, id, actor.getId(), amount);
                if (rows == 0) {
                    throw new BusinessException("申报已提交或状态已变化，不能重复提交");
                }
                auditService.record(connection, actor, "DECLARATION_SUBMIT", "DECLARATION",
                        String.valueOf(id), true, "提交申报 " + declaration.getDeclarationNo(),
                        traceId);
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void withdraw(SessionUser actor, long id, String traceId) throws SQLException {
        if (!actor.hasPermission(PermissionCodes.DECLARATION_SUBMIT)) {
            throw new ForbiddenException("无权限撤回申报");
        }
        try (Connection connection = Db.getConnection()) {
            TaxDeclaration declaration = declarationDao.findById(connection, id);
            if (declaration == null) {
                throw new NotFoundException("申报记录不存在");
            }
            if (declaration.getCreatedBy() != actor.getId() && !actor.isAdmin()) {
                throw new ForbiddenException("只能撤回本人创建的申报");
            }
            if (!declaration.isPending()) {
                throw new BusinessException("只有待审核的申报可以撤回");
            }
            int rows = declarationDao.withdraw(connection, id, actor.getId(), actor.isAdmin());
            if (rows == 0) {
                throw new BusinessException("该申报已进入审核流程或状态已变化，无法撤回");
            }
            auditService.record(connection, actor, "DECLARATION_WITHDRAW", "DECLARATION",
                    String.valueOf(id), true, "撤回申报 " + declaration.getDeclarationNo(), traceId);
        }
    }

    public List<TaxDeclaration> exportRows(SessionUser actor, DeclarationFilter filter,
                                           String traceId) throws SQLException {
        if (!actor.hasPermission(PermissionCodes.EXPORT_DECLARATION)) {
            throw new ForbiddenException("无权导出申报数据");
        }
        DeclarationFilter scoped = scopeFilter(actor, filter);
        try (Connection connection = Db.getConnection()) {
            List<TaxDeclaration> rows = declarationDao.search(connection, scoped, 0,
                    MAX_EXPORT_ROWS);
            auditService.record(connection, actor, "DECLARATION_EXPORT", "DECLARATION", null,
                    true, "导出申报数据 " + rows.size() + " 条", traceId);
            return rows;
        }
    }

    public List<FormField> templateFields(long templateId) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return templateDao.listFields(connection, templateId);
        }
    }

    public Map<String, String> validateFieldValues(List<FormField> fields,
                                                   Map<String, String> values) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FormField field : fields) {
            if (!field.isEnabled()) {
                continue;
            }
            String raw = values.get(field.getFieldCode());
            String value = raw == null ? "" : raw.trim();
            if (value.isEmpty()) {
                if (field.isRequired()) {
                    errors.put(field.getFieldCode(), field.getFieldLabel() + "为必填项");
                }
                continue;
            }
            if (value.length() > 2000) {
                errors.put(field.getFieldCode(), field.getFieldLabel() + "内容过长");
                continue;
            }
            switch (field.getFieldType()) {
                case FormField.TYPE_TEXT:
                    int maxLength = field.getMaxLength() == null ? 2000 : field.getMaxLength();
                    if (value.length() > maxLength) {
                        errors.put(field.getFieldCode(), field.getFieldLabel()
                                + "不能超过 " + maxLength + " 个字符");
                    }
                    break;
                case FormField.TYPE_INTEGER:
                    if (!INTEGER_PATTERN.matcher(value).matches()) {
                        errors.put(field.getFieldCode(), field.getFieldLabel() + "必须为整数");
                    } else {
                        checkRange(field, new BigDecimal(value), errors);
                    }
                    break;
                case FormField.TYPE_AMOUNT:
                    if (!AMOUNT_PATTERN.matcher(value).matches()) {
                        errors.put(field.getFieldCode(),
                                field.getFieldLabel() + "必须为不超过两位小数的非负金额");
                    } else {
                        checkRange(field, new BigDecimal(value), errors);
                    }
                    break;
                case FormField.TYPE_DATE:
                    if (DateUtil.parseDate(value) == null) {
                        errors.put(field.getFieldCode(), field.getFieldLabel() + "日期格式应为 yyyy-MM-dd");
                    }
                    break;
                case FormField.TYPE_RADIO:
                    if (!field.getOptionList().contains(value)) {
                        errors.put(field.getFieldCode(), field.getFieldLabel() + "取值不在可选范围内");
                    }
                    break;
                case FormField.TYPE_MULTI:
                    List<String> selected = splitMulti(value);
                    if (selected.isEmpty()) {
                        errors.put(field.getFieldCode(), field.getFieldLabel() + "至少选择一项");
                    } else if (!field.getOptionList().containsAll(selected)) {
                        errors.put(field.getFieldCode(), field.getFieldLabel() + "包含无效选项");
                    }
                    break;
                case FormField.TYPE_BOOLEAN:
                    if (!"true".equals(value) && !"false".equals(value)) {
                        errors.put(field.getFieldCode(), field.getFieldLabel() + "取值不合法");
                    }
                    break;
                default:
                    errors.put(field.getFieldCode(), "不支持的字段类型");
            }
        }
        return errors;
    }

    private void checkRange(FormField field, BigDecimal value, Map<String, String> errors) {
        if (field.getMinValue() != null && value.compareTo(field.getMinValue()) < 0) {
            errors.put(field.getFieldCode(), field.getFieldLabel()
                    + "不能小于 " + field.getMinValue().toPlainString());
        }
        if (field.getMaxValue() != null && value.compareTo(field.getMaxValue()) > 0) {
            errors.put(field.getFieldCode(), field.getFieldLabel()
                    + "不能大于 " + field.getMaxValue().toPlainString());
        }
    }

    private Map<String, String> normalizeValues(List<FormField> fields,
                                                Map<String, String> input) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (FormField field : fields) {
            if (!field.isEnabled()) {
                continue;
            }
            String raw = input.get(field.getFieldCode());
            String value = raw == null ? "" : raw.trim();
            if (value.length() > 2000) {
                errors.put(field.getFieldCode(), field.getFieldLabel() + "内容过长");
                continue;
            }
            values.put(field.getFieldCode(), value);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("申报表单内容有误", errors);
        }
        return values;
    }

    public BigDecimal computeAmount(List<FormField> fields, Map<String, String> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (FormField field : fields) {
            if (!field.isEnabled() || !FormField.TYPE_AMOUNT.equals(field.getFieldType())) {
                continue;
            }
            String raw = values.get(field.getFieldCode());
            if (raw == null || !AMOUNT_PATTERN.matcher(raw.trim()).matches()) {
                continue;
            }
            total = total.add(new BigDecimal(raw.trim()));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public List<String> splitMulti(String value) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private void validateBasic(DeclarationForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getTaxpayerId() == null || form.getTaxpayerId() <= 0) {
            errors.put("taxpayerId", "请选择纳税人");
        }
        if (form.getDeclarationTypeId() == null || form.getDeclarationTypeId() <= 0) {
            errors.put("declarationTypeId", "请选择申报类型");
        }
        LocalDate start = form.getPeriodStart();
        LocalDate end = form.getPeriodEnd();
        if (start == null) {
            errors.put("periodStart", "请选择申报期间开始日期");
        }
        if (end == null) {
            errors.put("periodEnd", "请选择申报期间结束日期");
        }
        if (start != null && end != null) {
            if (start.isAfter(end)) {
                errors.put("periodEnd", "期间结束日期不能早于开始日期");
            }
            if (end.isAfter(LocalDate.now())) {
                errors.put("periodEnd", "申报期间不能晚于今天");
            }
            if (start.plusYears(1).isBefore(end)) {
                errors.put("periodEnd", "申报期间跨度不能超过一年");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("请检查申报基本信息", errors);
        }
    }

    private DeclarationFilter scopeFilter(SessionUser actor, DeclarationFilter filter) {
        DeclarationFilter scoped = filter == null ? new DeclarationFilter() : filter;
        if (!canViewAll(actor)) {
            scoped.setCreatedBy(actor.getId());
        }
        return scoped;
    }

    private boolean canViewAll(SessionUser actor) {
        return actor != null && (actor.isAdmin() || actor.hasRole("REVIEWER")
                || actor.hasRole("ANALYST") || actor.hasRole("VIEWER"));
    }

    private void requireViewPermission(SessionUser actor) {
        if (actor == null || (!actor.hasPermission(PermissionCodes.DECLARATION_VIEW)
                && !actor.hasPermission(PermissionCodes.DECLARATION_CREATE))) {
            throw new ForbiddenException("无权查看申报");
        }
    }

    private long requireId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new ValidationException(message, Map.of("taxpayerId", message));
        }
        return value;
    }

    private String generateDeclarationNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        StringBuilder suffix = new StringBuilder();
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < 6; i++) {
            suffix.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return "TD" + date + "-" + suffix;
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // handled by servlet error page
        }
    }
}
