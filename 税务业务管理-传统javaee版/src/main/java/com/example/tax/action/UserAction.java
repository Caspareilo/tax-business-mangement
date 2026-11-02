package com.example.tax.action;

import com.example.tax.dto.Page;
import com.example.tax.dto.UserFilter;
import com.example.tax.exception.AppException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.Role;
import com.example.tax.model.User;
import com.example.tax.security.SessionUser;
import com.example.tax.service.UserService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        try {
            if ("/edit".equals(path)) {
                showEditForm(request, response);
            } else if (path.isEmpty()) {
                showList(request, response);
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/admin/users.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser actor = currentUser(request);
        try {
            if ("/save".equals(path)) {
                saveUser(request, response, actor);
            } else if ("/status".equals(path)) {
                userService.setStatus(actor, WebUtil.longParam(request, "id", 0),
                        WebUtil.trim(request, "status"), traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/admin/users.action", "用户状态已更新");
            } else if ("/reset-password".equals(path)) {
                String generated = userService.resetPassword(actor,
                        WebUtil.longParam(request, "id", 0),
                        WebUtil.trim(request, "password"), traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/admin/users.action",
                        "密码已重置。新密码（仅显示一次）：" + generated);
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
            handleAppException(request, response, e, "/admin/users.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        UserFilter filter = new UserFilter();
        filter.setKeyword(WebUtil.trim(request, "keyword"));
        filter.setStatus(WebUtil.trim(request, "status"));
        filter.setRoleId(WebUtil.optionalLong(request, "roleId"));
        int page = WebUtil.pageParam(request);
        int size = WebUtil.pageSizeParam(request);
        Page<User> users = userService.list(filter, page, size);
        request.setAttribute("pageData", users);
        request.setAttribute("filter", filter);
        request.setAttribute("allRoles", userService.listRoles());
        request.setAttribute("filterQuery", WebUtil.filterQuery(request, "keyword", "status", "roleId"));
        forward(request, response, "admin/users.jsp");
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        User user = new User();
        user.setStatus("ACTIVE");
        if (id != null) {
            user = userService.findById(id);
        }
        request.setAttribute("formUser", user);
        request.setAttribute("allRoles", userService.listRoles());
        request.setAttribute("isNew", id == null);
        forward(request, response, "admin/user-form.jsp");
    }

    private void saveUser(HttpServletRequest request, HttpServletResponse response,
                          SessionUser actor) throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        String username = WebUtil.trim(request, "username");
        String displayName = WebUtil.trim(request, "displayName");
        String password = request.getParameter("password") == null
                ? "" : request.getParameter("password");
        String status = WebUtil.trim(request, "status");
        List<Long> roleIds = parseLongs(request.getParameterValues("roleIds"));
        request.setAttribute("username", username);
        if (id == null) {
            userService.create(actor, username, displayName, password, roleIds, traceId(request));
            WebUtil.redirectWithSuccess(request, response, "/admin/users.action", "用户已创建");
        } else {
            userService.update(actor, id, displayName,
                    status.isEmpty() ? "ACTIVE" : status, roleIds, traceId(request));
            WebUtil.redirectWithSuccess(request, response, "/admin/users.action", "用户信息已更新");
        }
    }

    private void renderFormWithErrors(HttpServletRequest request, HttpServletResponse response,
                                      ValidationException exception)
            throws SQLException, ServletException, IOException {
        User user = new User();
        Long id = WebUtil.optionalLong(request, "id");
        if (id != null) {
            user.setId(id);
            User existing = userService.findById(id);
            user.setUsername(existing.getUsername());
        }
        user.setDisplayName(WebUtil.trim(request, "displayName"));
        user.setUsername(id == null ? WebUtil.trim(request, "username") : user.getUsername());
        user.setStatus(WebUtil.trim(request, "status").isEmpty() ? "ACTIVE"
                : WebUtil.trim(request, "status"));
        List<Long> roleIds = parseLongs(request.getParameterValues("roleIds"));
        List<Role> allRoles = userService.listRoles();
        List<Role> selected = new ArrayList<>();
        for (Role role : allRoles) {
            if (roleIds.contains(role.getId())) {
                selected.add(role);
            }
        }
        user.setRoles(selected);
        request.setAttribute("formUser", user);
        request.setAttribute("allRoles", allRoles);
        request.setAttribute("isNew", id == null);
        request.setAttribute("fieldErrors", exception.getFieldErrors());
        request.setAttribute("errorMessage", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forward(request, response, "admin/user-form.jsp");
    }

    private List<Long> parseLongs(String[] values) {
        List<Long> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            try {
                result.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                // ignore malformed id; validation handles the empty selection
            }
        }
        return result;
    }
}
