package com.example.tax.action;

import com.example.tax.exception.AppException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.DictItem;
import com.example.tax.security.SessionUser;
import com.example.tax.service.SystemService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DictionaryAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final SystemService systemService = new SystemService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!pathInfo(request).isEmpty()) {
            renderNotFound(request, response, "页面不存在");
            return;
        }
        try {
            List<DictItem> items = systemService.listDictionaries();
            Long editId = WebUtil.optionalLong(request, "editId");
            DictItem editing = null;
            if (editId != null) {
                for (DictItem item : items) {
                    if (item.getId() == editId) {
                        editing = item;
                    }
                }
            }
            request.setAttribute("items", items);
            request.setAttribute("editing", editing);
            forward(request, response, "admin/dictionaries.jsp");
        } catch (SQLException e) {
            renderServerError(request, response, e);
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
                systemService.saveDictionary(actor, id,
                        WebUtil.trim(request, "dictType"),
                        WebUtil.trim(request, "dictKey"),
                        WebUtil.trim(request, "dictLabel"),
                        WebUtil.intParam(request, "sortOrder", 0), traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/admin/dictionaries.action",
                        id == null ? "字典项已新增" : "字典项已更新");
            } else if ("/status".equals(path)) {
                boolean enabled = "1".equals(WebUtil.trim(request, "enabled"));
                systemService.setDictionaryEnabled(actor,
                        WebUtil.longParam(request, "id", 0), enabled, traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/admin/dictionaries.action",
                        enabled ? "字典项已启用" : "字典项已停用");
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (ValidationException e) {
            try {
                request.setAttribute("items", systemService.listDictionaries());
            } catch (SQLException ignored) {
                // the page will still render the error message
            }
            request.setAttribute("fieldErrors", e.getFieldErrors());
            request.setAttribute("errorMessage", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            forward(request, response, "admin/dictionaries.jsp");
        } catch (AppException e) {
            handleAppException(request, response, e, "/admin/dictionaries.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }
}
