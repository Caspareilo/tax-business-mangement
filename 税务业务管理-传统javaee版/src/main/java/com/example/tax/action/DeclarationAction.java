package com.example.tax.action;

import com.example.tax.dto.DeclarationFilter;
import com.example.tax.dto.DeclarationForm;
import com.example.tax.dto.Page;
import com.example.tax.exception.AppException;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.DeclarationType;
import com.example.tax.model.FormField;
import com.example.tax.model.FormTemplate;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.model.Taxpayer;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.service.DeclarationService;
import com.example.tax.service.DeclarationTypeService;
import com.example.tax.service.TaxpayerService;
import com.example.tax.util.DateUtil;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeclarationAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final DeclarationService declarationService = new DeclarationService();
    private final TaxpayerService taxpayerService = new TaxpayerService();
    private final DeclarationTypeService typeService = new DeclarationTypeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser user = currentUser(request);
        try {
            if ("/create".equals(path)) {
                showCreateForm(request, response, user);
            } else if ("/edit".equals(path)) {
                showEditForm(request, response, user);
            } else if ("/detail".equals(path)) {
                long id = WebUtil.longParam(request, "id", 0);
                request.setAttribute("declaration", declarationService.findDetail(id, user));
                request.setAttribute("canEdit", user.hasPermission(PermissionCodes.DECLARATION_SUBMIT));
                forward(request, response, "declarations/detail.jsp");
            } else if (path.isEmpty()) {
                showMine(request, response, user);
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/declarations.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser actor = currentUser(request);
        try {
            if ("/save".equals(path)) {
                saveDeclaration(request, response, actor);
            } else if ("/submit".equals(path)) {
                long id = WebUtil.longParam(request, "id", 0);
                declarationService.submit(actor, id, traceId(request));
                WebUtil.redirectWithSuccess(request, response,
                        "/declarations/detail.action?id=" + id, "申报已提交，等待审核");
            } else if ("/withdraw".equals(path)) {
                long id = WebUtil.longParam(request, "id", 0);
                declarationService.withdraw(actor, id, traceId(request));
                WebUtil.redirectWithSuccess(request, response,
                        "/declarations/detail.action?id=" + id, "申报已撤回");
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (ValidationException e) {
            Long id = WebUtil.optionalLong(request, "id");
            if (id != null && "submit".equals(path)) {
                WebUtil.flashError(request, joinValidationMessages(e));
                WebUtil.redirect(request, response, "/declarations/edit.action?id=" + id);
            } else if ("/save".equals(path)) {
                try {
                    renderFormWithErrors(request, response, e, actor);
                } catch (SQLException sqlException) {
                    renderServerError(request, response, sqlException);
                }
            } else {
                handleAppException(request, response, e, "/declarations.action");
            }
        } catch (BusinessException e) {
            if ("/submit".equals(path) || "/withdraw".equals(path)) {
                Long id = WebUtil.optionalLong(request, "id");
                WebUtil.flashError(request, e.getMessage());
                WebUtil.redirect(request, response,
                        id == null ? "/declarations.action" : "/declarations/detail.action?id=" + id);
            } else {
                handleAppException(request, response, e, "/declarations.action");
            }
        } catch (AppException e) {
            handleAppException(request, response, e, "/declarations.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private void showMine(HttpServletRequest request, HttpServletResponse response,
                          SessionUser user) throws SQLException, ServletException, IOException {
        DeclarationFilter filter = new DeclarationFilter();
        filter.setKeyword(WebUtil.trim(request, "keyword"));
        filter.setStatus(WebUtil.trim(request, "status"));
        filter.setDeclarationTypeId(WebUtil.optionalLong(request, "typeId"));
        Page<TaxDeclaration> pageData = declarationService.listMine(user, filter,
                WebUtil.pageParam(request), WebUtil.pageSizeParam(request));
        request.setAttribute("pageData", pageData);
        request.setAttribute("filter", filter);
        request.setAttribute("types", typeService.listAll());
        request.setAttribute("filterQuery",
                WebUtil.filterQuery(request, "keyword", "status", "typeId"));
        forward(request, response, "declarations/list.jsp");
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response,
                                SessionUser user) throws SQLException, ServletException, IOException {
        Long typeId = WebUtil.optionalLong(request, "typeId");
        DeclarationForm form = new DeclarationForm();
        form.setDeclarationTypeId(typeId);
        LocalDate today = LocalDate.now();
        form.setPeriodStart(YearMonth.from(today).atDay(1));
        form.setPeriodEnd(today);
        FormTemplate template = null;
        if (typeId != null) {
            try {
                template = findTemplateForType(typeId);
            } catch (BusinessException e) {
                request.setAttribute("errorMessage", e.getMessage());
            }
        }
        prepareFormAttributes(request, user, form, template);
        forward(request, response, "declarations/form.jsp");
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response,
                              SessionUser user) throws SQLException, ServletException, IOException {
        long id = WebUtil.longParam(request, "id", 0);
        TaxDeclaration declaration = declarationService.findForEdit(id, user);
        DeclarationForm form = new DeclarationForm();
        form.setId(declaration.getId());
        form.setTaxpayerId(declaration.getTaxpayerId());
        form.setDeclarationTypeId(declaration.getDeclarationTypeId());
        form.setPeriodStart(declaration.getPeriodStart());
        form.setPeriodEnd(declaration.getPeriodEnd());
        form.setFormValues(declaration.getFormValues());
        FormTemplate template = new FormTemplate();
        template.setId(declaration.getTemplateId());
        template.setVersionNo(declaration.getTemplateVersionNo());
        template.setFields(declaration.getTemplateFields());
        template.setDeclarationTypeId(declaration.getDeclarationTypeId());
        prepareFormAttributes(request, user, form, template);
        request.setAttribute("declaration", declaration);
        forward(request, response, "declarations/form.jsp");
    }

    private void prepareFormAttributes(HttpServletRequest request, SessionUser user,
                                       DeclarationForm form, FormTemplate template)
            throws SQLException {
        List<Taxpayer> taxpayers = new ArrayList<>(taxpayerService.listActive());
        if (form.getTaxpayerId() != null) {
            boolean present = taxpayers.stream().anyMatch(
                    t -> t.getId() == form.getTaxpayerId());
            if (!present) {
                try {
                    taxpayers.add(taxpayerService.findById(form.getTaxpayerId()));
                } catch (AppException ignored) {
                    // the taxpayer may have been removed; validation will report it
                }
            }
        }
        List<DeclarationType> types = new ArrayList<>(typeService.listEnabled());
        if (form.getDeclarationTypeId() != null) {
            boolean present = types.stream().anyMatch(
                    t -> t.getId() == form.getDeclarationTypeId());
            if (!present) {
                try {
                    types.add(typeService.findById(form.getDeclarationTypeId()));
                } catch (AppException ignored) {
                    // the type may have been disabled; validation will report it
                }
            }
        }
        request.setAttribute("taxpayers", taxpayers);
        request.setAttribute("types", types);
        request.setAttribute("form", form);
        request.setAttribute("template", template);
        request.setAttribute("currentUser", user);
    }

    private void saveDeclaration(HttpServletRequest request, HttpServletResponse response,
                                 SessionUser actor) throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        DeclarationForm form = new DeclarationForm();
        form.setId(id);
        form.setTaxpayerId(WebUtil.optionalLong(request, "taxpayerId"));
        form.setDeclarationTypeId(WebUtil.optionalLong(request, "declarationTypeId"));
        form.setPeriodStart(DateUtil.parseDate(WebUtil.trim(request, "periodStart")));
        form.setPeriodEnd(DateUtil.parseDate(WebUtil.trim(request, "periodEnd")));

        FormTemplate template = resolveTemplate(id, form.getDeclarationTypeId(), actor);
        form.setFormValues(readFormValues(template == null ? List.of() : template.getFields(),
                request));
        long savedId = declarationService.saveDraft(actor, form, traceId(request));
        boolean submitNow = "submit".equals(WebUtil.trim(request, "action"));
        if (submitNow) {
            try {
                declarationService.submit(actor, savedId, traceId(request));
                WebUtil.redirectWithSuccess(request, response,
                        "/declarations/detail.action?id=" + savedId, "申报已提交，等待审核");
            } catch (ValidationException e) {
                WebUtil.flashError(request, joinValidationMessages(e));
                WebUtil.redirect(request, response, "/declarations/edit.action?id=" + savedId);
            } catch (BusinessException e) {
                WebUtil.flashError(request, e.getMessage());
                WebUtil.redirect(request, response, "/declarations/edit.action?id=" + savedId);
            }
        } else {
            WebUtil.redirectWithSuccess(request, response,
                    "/declarations/detail.action?id=" + savedId, "申报草稿已保存");
        }
    }

    private void renderFormWithErrors(HttpServletRequest request, HttpServletResponse response,
                                      ValidationException exception, SessionUser actor)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        DeclarationForm form = new DeclarationForm();
        form.setId(id);
        form.setTaxpayerId(WebUtil.optionalLong(request, "taxpayerId"));
        form.setDeclarationTypeId(WebUtil.optionalLong(request, "declarationTypeId"));
        form.setPeriodStart(DateUtil.parseDate(WebUtil.trim(request, "periodStart")));
        form.setPeriodEnd(DateUtil.parseDate(WebUtil.trim(request, "periodEnd")));
        FormTemplate template = resolveTemplate(id, form.getDeclarationTypeId(), actor);
        form.setFormValues(readFormValues(template == null ? List.of() : template.getFields(),
                request));
        prepareFormAttributes(request, actor, form, template);
        request.setAttribute("fieldErrors", exception.getFieldErrors());
        request.setAttribute("errorMessage", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forward(request, response, "declarations/form.jsp");
    }

    private FormTemplate resolveTemplate(Long declarationId, Long typeId, SessionUser actor)
            throws SQLException {
        if (declarationId != null) {
            try {
                TaxDeclaration existing = declarationService.findForEdit(declarationId, actor);
                if (existing.getDeclarationTypeId() == (typeId == null ? -1 : typeId)) {
                    FormTemplate template = new FormTemplate();
                    template.setId(existing.getTemplateId());
                    template.setVersionNo(existing.getTemplateVersionNo());
                    template.setDeclarationTypeId(existing.getDeclarationTypeId());
                    template.setFields(existing.getTemplateFields());
                    return template;
                }
            } catch (AppException ignored) {
                // fall through to the published template lookup
            }
        }
        if (typeId == null) {
            return null;
        }
        try {
            return findTemplateForType(typeId);
        } catch (BusinessException e) {
            return null;
        }
    }

    private FormTemplate findTemplateForType(long typeId) throws SQLException {
        return declarationService.findPublishedTemplate(typeId);
    }

    private Map<String, String> readFormValues(List<FormField> fields,
                                               HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        for (FormField field : fields) {
            String parameterName = "field_" + field.getFieldCode();
            String[] raw = request.getParameterValues(parameterName);
            if (raw == null) {
                values.put(field.getFieldCode(), "");
                continue;
            }
            if (FormField.TYPE_BOOLEAN.equals(field.getFieldType())) {
                boolean checked = false;
                for (String value : raw) {
                    if ("true".equals(value)) {
                        checked = true;
                    }
                }
                values.put(field.getFieldCode(), checked ? "true" : "false");
            } else {
                StringBuilder joined = new StringBuilder();
                for (String value : raw) {
                    if (!value.trim().isEmpty()) {
                        if (joined.length() > 0) {
                            joined.append(',');
                        }
                        joined.append(value.trim());
                    }
                }
                values.put(field.getFieldCode(), joined.toString());
            }
        }
        return values;
    }

    private String joinValidationMessages(ValidationException exception) {
        StringBuilder message = new StringBuilder(exception.getMessage());
        for (String fieldMessage : exception.getFieldErrors().values()) {
            message.append("；").append(fieldMessage);
        }
        return message.toString();
    }
}
