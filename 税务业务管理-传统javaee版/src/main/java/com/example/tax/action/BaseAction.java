package com.example.tax.action;

import com.example.tax.exception.AppException;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.exception.ValidationException;
import com.example.tax.security.SessionUser;
import com.example.tax.util.WebUtil;
import com.opensymphony.xwork2.ActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Shared Struts 2 action base for the traditional Servlet/JSP stack.
 *
 * <p>The project deliberately targets Struts 6.x and Tomcat 9 because both use the
 * {@code javax.servlet} namespace. The action lifecycle, interceptors, routing and
 * result rendering are managed by Struts; this class only centralises common request
 * helpers used by the teaching examples.</p>
 */
public abstract class BaseAction extends ActionSupport {

    private static final long serialVersionUID = 1L;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Sub-operation configured in struts.xml. For example, the
     * {@code declarations/create} action sets this value to {@code create}.
     */
    protected String operation = "";
    private String resultView;

    /**
     * Struts entry point. Existing teaching examples keep the familiar GET/POST
     * method split while the request itself is dispatched by Struts.
     */
    @Override
    public String execute() throws Exception {
        HttpServletRequest request = org.apache.struts2.ServletActionContext.getRequest();
        HttpServletResponse response = org.apache.struts2.ServletActionContext.getResponse();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)) {
            doPost(request, response);
        } else if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            doGet(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            request.setAttribute("errorMessage", "不支持的请求方法");
            resultView = "error/405.jsp";
        }
        return resultView == null ? NONE : SUCCESS;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        request.setAttribute("errorMessage", "该操作不支持 GET 请求");
        resultView = "error/405.jsp";
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        request.setAttribute("errorMessage", "该操作不支持 POST 请求");
        resultView = "error/405.jsp";
    }

    public void setOperation(String operation) {
        this.operation = operation == null ? "" : operation.trim();
    }

    public String getResultView() {
        return resultView;
    }

    protected SessionUser currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute("currentUser");
    }

    protected String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute("traceId");
        return traceId == null ? "" : traceId.toString();
    }

    protected String pathInfo(HttpServletRequest request) {
        return operation == null || operation.isEmpty() ? "" : "/" + operation;
    }

    protected void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        resultView = view;
    }

    protected void renderForbidden(HttpServletRequest request, HttpServletResponse response,
                                   String message) throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        forward(request, response, "error/403.jsp");
    }

    protected void renderNotFound(HttpServletRequest request, HttpServletResponse response,
                                  String message) throws ServletException, IOException {
        request.setAttribute("errorMessage", message);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        forward(request, response, "error/404.jsp");
    }

    protected void renderServerError(HttpServletRequest request, HttpServletResponse response,
                                     Exception exception) throws ServletException, IOException {
        logger.error("请求处理失败 traceId={} uri={}", traceId(request),
                request.getRequestURI(), exception);
        request.setAttribute("errorMessage", "系统暂时无法处理该请求，请稍后重试或联系管理员");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        forward(request, response, "error/500.jsp");
    }

    /**
     * Common fallback for actions without a re-rendering form. Validation and business
     * failures become a flash message and a redirect; authorization failures show 403.
     */
    protected void handleAppException(HttpServletRequest request, HttpServletResponse response,
                                      AppException exception, String redirectPath)
            throws ServletException, IOException {
        if (exception instanceof ForbiddenException) {
            renderForbidden(request, response, exception.getMessage());
            return;
        }
        if (exception instanceof NotFoundException) {
            renderNotFound(request, response, exception.getMessage());
            return;
        }
        if (exception instanceof ValidationException) {
            ValidationException validation = (ValidationException) exception;
            StringBuilder message = new StringBuilder(exception.getMessage());
            for (String fieldMessage : validation.getFieldErrors().values()) {
                message.append("；").append(fieldMessage);
            }
            WebUtil.flashError(request, message.toString());
        } else if (exception instanceof BusinessException) {
            WebUtil.flashError(request, exception.getMessage());
        } else {
            WebUtil.flashError(request, "操作失败，请稍后重试");
        }
        WebUtil.redirect(request, response, redirectPath);
    }
}
