package com.example.tax.action;

import com.example.tax.exception.BusinessException;
import com.example.tax.security.CsrfUtil;
import com.example.tax.security.SessionUser;
import com.example.tax.service.AuthService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

public class LoginAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentUser(request) != null) {
            WebUtil.redirect(request, response, "/dashboard.action");
            return;
        }
        if ("1".equals(request.getParameter("logout"))) {
            request.setAttribute("infoMessage", "您已安全退出系统");
        }
        forward(request, response, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = WebUtil.trim(request, "username");
        String password = request.getParameter("password") == null
                ? "" : request.getParameter("password");
        request.setAttribute("username", username);
        if (username.isEmpty() || password.isEmpty()) {
            request.setAttribute("errorMessage", "请输入账号和密码");
            forward(request, response, "login.jsp");
            return;
        }
        try {
            SessionUser sessionUser = authService.login(username, password, traceId(request));
            HttpSession session = request.getSession(false);
            if (session == null) {
                session = request.getSession(true);
            }
            request.changeSessionId();
            session.setAttribute("currentUser", sessionUser);
            session.removeAttribute(CsrfUtil.SESSION_KEY);
            session.removeAttribute("flashSuccess");
            session.removeAttribute("flashError");
            String target = (String) session.getAttribute("redirectAfterLogin");
            session.removeAttribute("redirectAfterLogin");
            if (target != null && target.startsWith("/") && !target.startsWith("//")
                    && !target.startsWith("/auth/")) {
                WebUtil.redirect(request, response, target);
            } else {
                WebUtil.redirect(request, response, "/dashboard.action");
            }
        } catch (BusinessException e) {
            request.setAttribute("errorMessage", e.getMessage());
            forward(request, response, "login.jsp");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }
}
