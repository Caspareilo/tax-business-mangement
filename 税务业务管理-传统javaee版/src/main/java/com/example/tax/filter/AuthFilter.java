package com.example.tax.filter;

import com.example.tax.security.CsrfUtil;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Authentication, coarse-grained URL permission checks and CSRF validation.
 * Fine-grained business rules are enforced again in the service layer.
 */
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // no configuration
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = requestPath(req);

        if (isStaticAsset(path) || "/favicon.ico".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(true);
        req.setAttribute("csrfToken", CsrfUtil.getToken(session));

        boolean loginPage = "/auth/login".equals(path);
        SessionUser user = (SessionUser) session.getAttribute("currentUser");
        req.setAttribute("currentUser", user);

        if (loginPage) {
            if ("POST".equalsIgnoreCase(req.getMethod()) && !CsrfUtil.isValid(req)) {
                req.setAttribute("errorMessage", "安全校验失败（CSRF Token 缺失或过期），请重新打开登录页");
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (user == null) {
            if ("GET".equalsIgnoreCase(req.getMethod())) {
                String target = path + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
                session.setAttribute("redirectAfterLogin", target);
            }
            resp.sendRedirect(req.getContextPath() + "/auth/login.action");
            return;
        }

        if ("POST".equalsIgnoreCase(req.getMethod()) && !CsrfUtil.isValid(req)) {
            req.setAttribute("errorMessage", "安全校验失败（CSRF Token 缺失或过期），请重新打开页面后提交");
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        String requiredPermission = requiredPermission(path, req.getMethod());
        if (requiredPermission != null && !user.hasPermission(requiredPermission)) {
            req.setAttribute("errorMessage", "当前账号没有访问该功能的权限");
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req, resp);
            return;
        }

        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        chain.doFilter(request, response);
    }

    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String path = uri.substring(context.length());
        if (path.isEmpty()) {
            return "/";
        }
        if (path.endsWith(".action")) {
            path = path.substring(0, path.length() - ".action".length());
        }
        return path;
    }

    private boolean isStaticAsset(String path) {
        return path.startsWith("/assets/");
    }

    private String requiredPermission(String path, String method) {
        if (path.startsWith("/admin/users")) {
            return PermissionCodes.USER_MANAGE;
        }
        if (path.startsWith("/admin/roles")) {
            return PermissionCodes.ROLE_MANAGE;
        }
        if (path.startsWith("/admin/dictionaries")) {
            return PermissionCodes.DICT_MANAGE;
        }
        if (path.startsWith("/admin/audit-logs")) {
            return PermissionCodes.AUDIT_VIEW;
        }
        if (path.startsWith("/form-templates")) {
            return PermissionCodes.TEMPLATE_MANAGE;
        }
        if (path.startsWith("/declaration-types/save")
                || path.startsWith("/declaration-types/status")) {
            return PermissionCodes.TYPE_MANAGE;
        }
        if (path.startsWith("/taxpayers/save") || path.startsWith("/taxpayers/status")) {
            return PermissionCodes.TAXPAYER_MANAGE;
        }
        if (path.startsWith("/declarations/submit") || path.startsWith("/declarations/withdraw")) {
            return PermissionCodes.DECLARATION_SUBMIT;
        }
        if (path.startsWith("/declarations/create") || path.startsWith("/declarations/edit")
                || path.startsWith("/declarations/save")) {
            return PermissionCodes.DECLARATION_CREATE;
        }
        if (path.startsWith("/declarations")) {
            return PermissionCodes.DECLARATION_VIEW;
        }
        if (path.startsWith("/reviews")) {
            return PermissionCodes.REVIEW_MANAGE;
        }
        if (path.startsWith("/statistics")) {
            return PermissionCodes.STATISTICS_VIEW;
        }
        if (path.startsWith("/search/export")) {
            return PermissionCodes.EXPORT_DECLARATION;
        }
        if (path.startsWith("/search")) {
            return PermissionCodes.DECLARATION_VIEW;
        }
        if (path.startsWith("/dashboard")) {
            return PermissionCodes.DASHBOARD_VIEW;
        }
        return null;
    }

    @Override
    public void destroy() {
        // nothing to release
    }
}
