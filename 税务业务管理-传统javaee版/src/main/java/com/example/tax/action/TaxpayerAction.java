package com.example.tax.action;

import com.example.tax.dto.DeclarationFilter;
import com.example.tax.dto.Page;
import com.example.tax.dto.TaxpayerFilter;
import com.example.tax.exception.AppException;
import com.example.tax.exception.ValidationException;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.model.Taxpayer;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.service.DeclarationService;
import com.example.tax.service.TaxpayerService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TaxpayerAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final TaxpayerService taxpayerService = new TaxpayerService();
    private final DeclarationService declarationService = new DeclarationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        if (user == null || !user.hasPermission(PermissionCodes.TAXPAYER_VIEW)) {
            renderForbidden(request, response, "无权查看纳税人信息");
            return;
        }
        String path = pathInfo(request);
        try {
            if ("/create".equals(path)) {
                Taxpayer taxpayer = new Taxpayer();
                taxpayer.setTaxpayerType("COMPANY");
                taxpayer.setRegistrationStatus("ACTIVE");
                request.setAttribute("formTaxpayer", taxpayer);
                request.setAttribute("isNew", true);
                forward(request, response, "taxpayers/form.jsp");
            } else if ("/edit".equals(path)) {
                Taxpayer taxpayer = taxpayerService.findById(
                        WebUtil.longParam(request, "id", 0));
                request.setAttribute("formTaxpayer", taxpayer);
                request.setAttribute("isNew", false);
                forward(request, response, "taxpayers/form.jsp");
            } else if ("/detail".equals(path)) {
                showDetail(request, response, user);
            } else if (path.isEmpty()) {
                showList(request, response);
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/taxpayers.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser actor = currentUser(request);
        try {
            if ("/save".equals(path)) {
                saveTaxpayer(request, response, actor);
            } else if ("/status".equals(path)) {
                taxpayerService.setStatus(actor, WebUtil.longParam(request, "id", 0),
                        WebUtil.trim(request, "status"), traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/taxpayers.action", "纳税人状态已更新");
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
            handleAppException(request, response, e, "/taxpayers.action");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        TaxpayerFilter filter = new TaxpayerFilter();
        filter.setKeyword(WebUtil.trim(request, "keyword"));
        filter.setTaxpayerType(WebUtil.trim(request, "taxpayerType"));
        filter.setStatus(WebUtil.trim(request, "status"));
        Page<Taxpayer> pageData = taxpayerService.list(filter, WebUtil.pageParam(request),
                WebUtil.pageSizeParam(request));
        request.setAttribute("pageData", pageData);
        request.setAttribute("filter", filter);
        request.setAttribute("filterQuery", WebUtil.filterQuery(request, "keyword",
                "taxpayerType", "status"));
        forward(request, response, "taxpayers/list.jsp");
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response,
                            SessionUser user) throws SQLException, ServletException, IOException {
        Taxpayer taxpayer = taxpayerService.findById(WebUtil.longParam(request, "id", 0));
        DeclarationFilter filter = new DeclarationFilter();
        filter.setTaxpayerId(taxpayer.getId());
        Page<TaxDeclaration> declarations = declarationService.list(user, filter,
                WebUtil.pageParam(request), 10);
        request.setAttribute("taxpayer", taxpayer);
        request.setAttribute("pageData", declarations);
        request.setAttribute("filterQuery", "");
        forward(request, response, "taxpayers/detail.jsp");
    }

    private void saveTaxpayer(HttpServletRequest request, HttpServletResponse response,
                              SessionUser actor) throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        Taxpayer taxpayer = buildFromRequest(request);
        if (id == null) {
            taxpayerService.create(actor, taxpayer, traceId(request));
            WebUtil.redirectWithSuccess(request, response, "/taxpayers.action", "纳税人已创建");
        } else {
            taxpayer.setId(id);
            taxpayerService.update(actor, taxpayer, traceId(request));
            WebUtil.redirectWithSuccess(request, response, "/taxpayers.action", "纳税人信息已更新");
        }
    }

    private Taxpayer buildFromRequest(HttpServletRequest request) {
        Taxpayer taxpayer = new Taxpayer();
        taxpayer.setTaxpayerCode(WebUtil.trim(request, "taxpayerCode"));
        taxpayer.setTaxpayerName(WebUtil.trim(request, "taxpayerName"));
        taxpayer.setTaxpayerType(WebUtil.trim(request, "taxpayerType"));
        taxpayer.setContactName(WebUtil.trim(request, "contactName"));
        taxpayer.setContactPhone(WebUtil.trim(request, "contactPhone"));
        taxpayer.setRegistrationStatus(WebUtil.trim(request, "registrationStatus"));
        taxpayer.setRemark(WebUtil.trim(request, "remark"));
        return taxpayer;
    }

    private void renderFormWithErrors(HttpServletRequest request, HttpServletResponse response,
                                      ValidationException exception)
            throws SQLException, ServletException, IOException {
        Long id = WebUtil.optionalLong(request, "id");
        Taxpayer taxpayer = buildFromRequest(request);
        if (id != null) {
            taxpayer.setId(id);
            Taxpayer existing = taxpayerService.findById(id);
            taxpayer.setTaxpayerCode(existing.getTaxpayerCode());
        }
        request.setAttribute("formTaxpayer", taxpayer);
        request.setAttribute("isNew", id == null);
        request.setAttribute("fieldErrors", exception.getFieldErrors());
        request.setAttribute("errorMessage", exception.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        forward(request, response, "taxpayers/form.jsp");
    }
}
