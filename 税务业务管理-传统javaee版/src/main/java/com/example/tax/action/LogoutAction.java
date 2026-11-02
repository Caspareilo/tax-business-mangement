package com.example.tax.action;

import com.example.tax.security.SessionUser;
import com.example.tax.service.AuditService;
import com.example.tax.util.Db;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;

public class LogoutAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final AuditService auditService = new AuditService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        if (user != null) {
            try (Connection connection = Db.getConnection()) {
                auditService.record(connection, user, "LOGOUT", "SESSION",
                        String.valueOf(user.getId()), true, "退出登录", traceId(request));
            } catch (Exception e) {
                logger.warn("记录退出日志失败: {}", e.getMessage());
            }
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        WebUtil.redirect(request, response, "/auth/login.action?logout=1");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // GET must never change state (session remains valid); send the user back to the app.
        WebUtil.redirect(request, response, "/dashboard.action");
    }
}
