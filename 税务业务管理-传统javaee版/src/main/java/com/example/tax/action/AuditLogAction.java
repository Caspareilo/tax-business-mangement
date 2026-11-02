package com.example.tax.action;

import com.example.tax.dto.AuditLogFilter;
import com.example.tax.dto.Page;
import com.example.tax.exception.AppException;
import com.example.tax.model.OperationLog;
import com.example.tax.service.AuditService;
import com.example.tax.util.DateUtil;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class AuditLogAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuditLogFilter filter = new AuditLogFilter();
        filter.setKeyword(WebUtil.trim(request, "keyword"));
        filter.setOperationType(WebUtil.trim(request, "operationType"));
        filter.setResult(WebUtil.trim(request, "result"));
        filter.setDateFrom(DateUtil.parseDate(WebUtil.trim(request, "dateFrom")));
        filter.setDateTo(DateUtil.parseDate(WebUtil.trim(request, "dateTo")));
        try {
            Page<OperationLog> pageData = auditService.search(filter,
                    WebUtil.pageParam(request), WebUtil.pageSizeParam(request));
            request.setAttribute("pageData", pageData);
            request.setAttribute("filter", filter);
            request.setAttribute("operationTypes", auditService.operationTypes());
            request.setAttribute("filterQuery", WebUtil.filterQuery(request, "keyword",
                    "operationType", "result", "dateFrom", "dateTo"));
            forward(request, response, "admin/audit-logs.jsp");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/dashboard.action");
        }
    }
}
