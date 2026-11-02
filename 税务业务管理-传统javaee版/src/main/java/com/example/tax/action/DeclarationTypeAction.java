package com.example.tax.action;

import com.example.tax.dto.Page;
import com.example.tax.exception.AppException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.DeclarationType;
import com.example.tax.security.SessionUser;
import com.example.tax.service.DeclarationTypeService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class DeclarationTypeAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final DeclarationTypeService typeService = new DeclarationTypeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        try {
            if ("/edit".equals(path)) {
                Long id = WebUtil.optionalLong(request, "id");
                DeclarationType type = new DeclarationType();
                type.setEnabled(true);
                type.setDeadlineDay(15);
                if (id != null) {
                    type = typeService.findById(id);
                }
                request.setAttribute("formType", type);
                request.setAttribute("isNew", id == null);
                forward(request, response, "declaration-types/form.jsp");
            } else if (path.isEmpty()) {
                String keyword = WebUtil.trim(request, "keyword");
                String enabled = WebUtil.trim(request, "enabled");
                Page<DeclarationType> pageData = typeService.list(keyword, enabled,
                        WebUtil.pageParam(request), WebUtil.pageSizeParam(request));
                request.setAttribute("pageData", pageData);
                request.setAttribute("keyword", keyword);
                request.setAttribute("enabledFilter", enabled);
                request.setAttribute("filterQuery",
                        WebUtil.filterQuery(request, "keyword", "enabled"));
                forward(request, response, "declaration-types/list.jsp");
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/declaration-types.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser actor = currentUser(request);
        try {
            if ("/save".equals(path)) {
                Long id = WebUtil.optionalLong(request, "id");
                Integer deadlineDay = parseInteger(request.getParameter("deadlineDay"));
                typeService.save(actor, id, WebUtil.trim(request, "typeCode"),
                        WebUtil.trim(request, "typeName"), WebUtil.trim(request, "description"),
                        deadlineDay, traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/declaration-types.action",
                        id == null ? "申报类型已创建" : "申报类型已更新");
            } else if ("/status".equals(path)) {
                boolean enabled = "1".equals(WebUtil.trim(request, "enabled"));
                typeService.setEnabled(actor, WebUtil.longParam(request, "id", 0), enabled,
                        traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/declaration-types.action",
                        enabled ? "申报类型已启用" : "申报类型已停用");
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
            handleAppException(request, response, e, "/declaration-types.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private void renderFormWithErrors(HttpServletRequest request, HttpServletResponse response,
                                      ValidationException exception)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        DeclarationType type = new DeclarationType();
        if (id != null) {
            DeclarationType existing = typeService.findById(id);
            type.setId(id);
            type.setTypeCode(existing.getTypeCode());
        } else {
            type.setTypeCode(WebUtil.trim(request, "typeCode"));
        }
        type.setTypeName(WebUtil.trim(request, "typeName"));
        type.setDescription(WebUtil.trim(request, "description"));
        type.setDeadlineDay(parseInteger(request.getParameter("deadlineDay")));
        type.setEnabled(true);
        request.setAttribute("formType", type);
        request.setAttribute("isNew", id == null);
        request.setAttribute("fieldErrors", exception.getFieldErrors());
        request.setAttribute("errorMessage", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forward(request, response, "declaration-types/form.jsp");
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
