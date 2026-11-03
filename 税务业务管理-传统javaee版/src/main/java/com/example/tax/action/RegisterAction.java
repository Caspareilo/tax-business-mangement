package com.example.tax.action;

import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ValidationException;
import com.example.tax.service.AuthService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Public self-registration entry for the traditional JSP login screen.
 */
public class RegisterAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (currentUser(request) != null) {
            WebUtil.redirect(request, response, "/dashboard.action");
            return;
        }
        request.setAttribute("activeLoginTab", "register");
        forward(request, response, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = WebUtil.trim(request, "username");
        String displayName = WebUtil.trim(request, "displayName");
        String password = request.getParameter("password") == null
                ? "" : request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword") == null
                ? "" : request.getParameter("confirmPassword");

        try {
            authService.register(username, displayName, password, confirmPassword,
                    traceId(request));
            String target = "/auth/login.action?registered=1&username="
                    + WebUtil.urlEncode(username);
            WebUtil.redirect(request, response, target);
        } catch (ValidationException e) {
            showRegistrationForm(request, response, username, displayName, e.getMessage(),
                    e.getFieldErrors());
        } catch (BusinessException e) {
            showRegistrationForm(request, response, username, displayName, e.getMessage(),
                    java.util.Map.of());
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private void showRegistrationForm(HttpServletRequest request, HttpServletResponse response,
                                      String username, String displayName, String errorMessage,
                                      java.util.Map<String, String> fieldErrors)
            throws ServletException, IOException {
        request.setAttribute("activeLoginTab", "register");
        request.setAttribute("registerUsername", username);
        request.setAttribute("registerDisplayName", displayName);
        request.setAttribute("registrationError", errorMessage);
        request.setAttribute("registrationFieldErrors", fieldErrors);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forward(request, response, "login.jsp");
    }
}
