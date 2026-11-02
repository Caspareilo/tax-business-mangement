package com.example.tax.action;

import com.example.tax.dto.FieldInput;
import com.example.tax.dto.Page;
import com.example.tax.exception.AppException;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.FormField;
import com.example.tax.model.FormTemplate;
import com.example.tax.security.SessionUser;
import com.example.tax.service.DeclarationTypeService;
import com.example.tax.service.FormTemplateService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FormTemplateAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final FormTemplateService templateService = new FormTemplateService();
    private final DeclarationTypeService typeService = new DeclarationTypeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        try {
            if ("/edit".equals(path)) {
                showEditForm(request, response);
            } else if ("/preview".equals(path)) {
                FormTemplate template = templateService.findById(
                        WebUtil.longParam(request, "id", 0));
                request.setAttribute("template", template);
                forward(request, response, "form-templates/preview.jsp");
            } else if ("/versions".equals(path)) {
                String code = WebUtil.trim(request, "code");
                request.setAttribute("templateCode", code);
                request.setAttribute("versions", templateService.listVersions(code));
                forward(request, response, "form-templates/versions.jsp");
            } else if (path.isEmpty()) {
                showList(request, response);
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/form-templates.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser actor = currentUser(request);
        try {
            if ("/save".equals(path)) {
                saveTemplate(request, response, actor);
            } else if ("/publish".equals(path)) {
                long id = WebUtil.longParam(request, "id", 0);
                templateService.publish(actor, id, traceId(request));
                WebUtil.redirectWithSuccess(request, response,
                        "/form-templates/preview.action?id=" + id, "模板已发布");
            } else if ("/disable".equals(path)) {
                templateService.disable(actor, WebUtil.longParam(request, "id", 0),
                        traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/form-templates.action", "模板已停用");
            } else if ("/new-version".equals(path)) {
                long id = templateService.createNewVersion(actor,
                        WebUtil.longParam(request, "id", 0), traceId(request));
                WebUtil.redirectWithSuccess(request, response,
                        "/form-templates/edit.action?id=" + id, "已创建新的草稿版本");
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (ValidationException e) {
            try {
                renderFormWithErrors(request, response, e);
            } catch (SQLException sqlException) {
                renderServerError(request, response, sqlException);
            }
        } catch (AppException e) {
            handleAppException(request, response, e, "/form-templates.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Long typeId = WebUtil.optionalLong(request, "typeId");
        String status = WebUtil.trim(request, "status");
        String keyword = WebUtil.trim(request, "keyword");
        Page<FormTemplate> pageData = templateService.list(typeId, status, keyword,
                WebUtil.pageParam(request), WebUtil.pageSizeParam(request));
        request.setAttribute("pageData", pageData);
        request.setAttribute("types", typeService.listAll());
        request.setAttribute("typeIdFilter", typeId);
        request.setAttribute("statusFilter", status);
        request.setAttribute("keyword", keyword);
        request.setAttribute("filterQuery",
                WebUtil.filterQuery(request, "typeId", "status", "keyword"));
        forward(request, response, "form-templates/list.jsp");
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        FormTemplate template = new FormTemplate();
        template.setStatus("DRAFT");
        if (id != null) {
            template = templateService.findById(id);
            if (!template.isDraft()) {
                WebUtil.redirectWithError(request, response,
                        "/form-templates/preview.action?id=" + id,
                        "已发布或已停用的模板不可编辑，请创建新版本");
                return;
            }
        } else {
            Long typeId = WebUtil.optionalLong(request, "typeId");
            if (typeId != null) {
                template.setDeclarationTypeId(typeId);
            }
        }
        request.setAttribute("template", template);
        request.setAttribute("types", typeService.listAll());
        request.setAttribute("isNew", id == null);
        forward(request, response, "form-templates/edit.jsp");
    }

    private void saveTemplate(HttpServletRequest request, HttpServletResponse response,
                              SessionUser actor) throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        Long typeId = WebUtil.optionalLong(request, "typeId");
        String templateCode = WebUtil.trim(request, "templateCode");
        String templateName = WebUtil.trim(request, "templateName");
        List<FieldInput> inputs = readFieldInputs(request);
        if (id == null) {
            long newId = templateService.createDraft(actor,
                    typeId == null ? 0L : typeId, templateCode, templateName, inputs,
                    traceId(request));
            WebUtil.redirectWithSuccess(request, response,
                    "/form-templates/edit.action?id=" + newId, "模板草稿已创建");
        } else {
            templateService.saveDraft(actor, id, templateName, inputs, traceId(request));
            WebUtil.redirectWithSuccess(request, response,
                    "/form-templates/edit.action?id=" + id, "模板草稿已保存");
        }
    }

    private void renderFormWithErrors(HttpServletRequest request, HttpServletResponse response,
                                      ValidationException exception)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        Long typeId = WebUtil.optionalLong(request, "typeId");
        FormTemplate template = new FormTemplate();
        template.setId(id == null ? 0 : id);
        template.setTemplateCode(WebUtil.trim(request, "templateCode"));
        template.setTemplateName(WebUtil.trim(request, "templateName"));
        template.setDeclarationTypeId(typeId == null ? 0 : typeId);
        if (id != null) {
            FormTemplate existing = templateService.findById(id);
            template.setTemplateCode(existing.getTemplateCode());
            template.setVersionNo(existing.getVersionNo());
            template.setDeclarationTypeId(existing.getDeclarationTypeId());
            template.setStatus(existing.getStatus());
        } else {
            template.setStatus("DRAFT");
        }
        List<FormField> fields = new ArrayList<>();
        int index = 0;
        for (FieldInput input : readFieldInputs(request)) {
            fields.add(input.toFormField(index++));
        }
        template.setFields(fields);
        request.setAttribute("template", template);
        request.setAttribute("types", typeService.listAll());
        request.setAttribute("isNew", id == null);
        request.setAttribute("fieldErrors", exception.getFieldErrors());
        request.setAttribute("errorMessage", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forward(request, response, "form-templates/edit.jsp");
    }

    private List<FieldInput> readFieldInputs(HttpServletRequest request) {
        String[] codes = request.getParameterValues("fieldCode");
        String[] labels = request.getParameterValues("fieldLabel");
        String[] types = request.getParameterValues("fieldType");
        String[] mins = request.getParameterValues("minValue");
        String[] maxs = request.getParameterValues("maxValue");
        String[] lengths = request.getParameterValues("maxLength");
        String[] options = request.getParameterValues("options");
        String[] helps = request.getParameterValues("helpText");
        String[] requiredIndexes = request.getParameterValues("fieldRequired");
        Set<Integer> required = new HashSet<>();
        if (requiredIndexes != null) {
            for (String value : requiredIndexes) {
                try {
                    required.add(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                    // ignore malformed input
                }
            }
        }
        int count = codes == null ? 0 : codes.length;
        List<FieldInput> inputs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            FieldInput input = new FieldInput();
            input.setFieldCode(value(codes, i));
            input.setFieldLabel(value(labels, i));
            input.setFieldType(value(types, i));
            input.setRequired(required.contains(i));
            input.setMinValue(value(mins, i));
            input.setMaxValue(value(maxs, i));
            input.setMaxLength(value(lengths, i));
            input.setOptions(value(options, i));
            input.setHelpText(value(helps, i));
            inputs.add(input);
        }
        return inputs;
    }

    private String value(String[] values, int index) {
        return values != null && index < values.length ? values[index] : "";
    }
}
