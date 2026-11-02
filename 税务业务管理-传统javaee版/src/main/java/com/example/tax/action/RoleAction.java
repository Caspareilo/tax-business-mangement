package com.example.tax.action;

import com.example.tax.exception.AppException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.Permission;
import com.example.tax.model.Role;
import com.example.tax.security.SessionUser;
import com.example.tax.service.RoleService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoleAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final RoleService roleService = new RoleService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        try {
            if ("/edit".equals(path)) {
                showEditForm(request, response);
            } else if (path.isEmpty()) {
                request.setAttribute("roles", roleService.listAll());
                forward(request, response, "admin/roles.jsp");
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/admin/roles.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser actor = currentUser(request);
        try {
            if ("/save".equals(path)) {
                saveRole(request, response, actor);
            } else if ("/status".equals(path)) {
                roleService.setStatus(actor, WebUtil.longParam(request, "id", 0),
                        WebUtil.trim(request, "status"), traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/admin/roles.action", "角色状态已更新");
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
            handleAppException(request, response, e, "/admin/roles.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        Role role = new Role();
        role.setStatus("ACTIVE");
        if (id != null) {
            role = roleService.findById(id);
        }
        request.setAttribute("formRole", role);
        request.setAttribute("permissions", roleService.listPermissions());
        request.setAttribute("isNew", id == null);
        forward(request, response, "admin/role-form.jsp");
    }

    private void saveRole(HttpServletRequest request, HttpServletResponse response,
                          SessionUser actor) throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        String roleCode = WebUtil.trim(request, "roleCode");
        String roleName = WebUtil.trim(request, "roleName");
        String description = WebUtil.trim(request, "description");
        String status = WebUtil.trim(request, "status");
        List<Long> permissionIds = parseLongs(request.getParameterValues("permissionIds"));
        roleService.save(actor, id, roleCode, roleName, description,
                status.isEmpty() ? "ACTIVE" : status, permissionIds, traceId(request));
        WebUtil.redirectWithSuccess(request, response, "/admin/roles.action",
                id == null ? "角色已创建" : "角色已更新");
    }

    private void renderFormWithErrors(HttpServletRequest request, HttpServletResponse response,
                                      ValidationException exception)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        Role role = new Role();
        if (id != null) {
            Role existing = roleService.findById(id);
            role.setId(id);
            role.setRoleCode(existing.getRoleCode());
        } else {
            role.setRoleCode(WebUtil.trim(request, "roleCode"));
        }
        role.setRoleName(WebUtil.trim(request, "roleName"));
        role.setDescription(WebUtil.trim(request, "description"));
        role.setStatus(WebUtil.trim(request, "status").isEmpty() ? "ACTIVE"
                : WebUtil.trim(request, "status"));
        List<Long> selectedIds = parseLongs(request.getParameterValues("permissionIds"));
        List<Permission> all = roleService.listPermissions();
        List<Permission> selected = new ArrayList<>();
        for (Permission permission : all) {
            if (selectedIds.contains(permission.getId())) {
                selected.add(permission);
            }
        }
        role.setPermissions(selected);
        role.setPermissionIds(selectedIds);
        request.setAttribute("formRole", role);
        request.setAttribute("permissions", all);
        request.setAttribute("isNew", id == null);
        request.setAttribute("fieldErrors", exception.getFieldErrors());
        request.setAttribute("errorMessage", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forward(request, response, "admin/role-form.jsp");
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
                // invalid ids are ignored; the service validates the final state
            }
        }
        return result;
    }
}
