package com.example.tax.action;

import com.example.tax.exception.AppException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.User;
import com.example.tax.security.SessionUser;
import com.example.tax.service.UserService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class ProfileAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        try {
            User profile = userService.findById(user.getId());
            request.setAttribute("profile", profile);
            forward(request, response, "profile.jsp");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/dashboard.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/password".equals(pathInfo(request))) {
            renderNotFound(request, response, "页面不存在");
            return;
        }
        SessionUser actor = currentUser(request);
        String oldPassword = request.getParameter("oldPassword") == null
                ? "" : request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword") == null
                ? "" : request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword") == null
                ? "" : request.getParameter("confirmPassword");
        if (!newPassword.equals(confirmPassword)) {
            WebUtil.flashError(request, "两次输入的新密码不一致");
            WebUtil.redirect(request, response, "/profile.action");
            return;
        }
        try {
            userService.changeOwnPassword(actor, oldPassword, newPassword, traceId(request));
            WebUtil.redirectWithSuccess(request, response, "/profile.action",
                    "密码已修改，请牢记新密码");
        } catch (ValidationException e) {
            WebUtil.flashError(request, e.getMessage());
            WebUtil.redirect(request, response, "/profile.action");
        } catch (AppException e) {
            handleAppException(request, response, e, "/profile.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }
}
