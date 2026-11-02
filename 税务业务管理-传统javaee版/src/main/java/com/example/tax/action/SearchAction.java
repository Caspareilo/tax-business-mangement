package com.example.tax.action;

import com.example.tax.dto.DeclarationFilter;
import com.example.tax.dto.Page;
import com.example.tax.exception.AppException;
import com.example.tax.model.Role;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.model.User;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.service.DeclarationService;
import com.example.tax.service.DeclarationTypeService;
import com.example.tax.service.UserService;
import com.example.tax.util.CsvUtil;
import com.example.tax.util.DateUtil;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final DeclarationService declarationService = new DeclarationService();
    private final DeclarationTypeService typeService = new DeclarationTypeService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        try {
            DeclarationFilter filter = buildFilter(request);
            int page = WebUtil.pageParam(request);
            int size = WebUtil.pageSizeParam(request);
            Page<TaxDeclaration> pageData = declarationService.list(user, filter, page, size);
            request.setAttribute("pageData", pageData);
            request.setAttribute("filter", filter);
            request.setAttribute("types", typeService.listAll());
            request.setAttribute("reviewers", listReviewers());
            request.setAttribute("canExport", user.hasPermission(PermissionCodes.EXPORT_DECLARATION));
            request.setAttribute("filterQuery", buildFilterQuery(request));
            forward(request, response, "search.jsp");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/search/declarations.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        try {
            DeclarationFilter filter = buildFilter(request);
            List<TaxDeclaration> rows = declarationService.exportRows(user, filter,
                    traceId(request));
            List<String> headers = Arrays.asList("申报编号", "纳税人编号", "纳税人名称", "申报类型",
                    "期间开始", "期间结束", "状态", "模拟金额", "经办人", "提交时间");
            List<List<String>> csvRows = new ArrayList<>();
            for (TaxDeclaration row : rows) {
                csvRows.add(Arrays.asList(
                        row.getDeclarationNo(),
                        row.getTaxpayerCode() == null ? "" : row.getTaxpayerCode(),
                        row.getTaxpayerNameSnapshot(),
                        row.getDeclarationTypeSnapshot(),
                        DateUtil.formatDate(row.getPeriodStart()),
                        DateUtil.formatDate(row.getPeriodEnd()),
                        row.getStatusLabel(),
                        row.getDeclaredAmount() == null ? "0.00" : row.getDeclaredAmount().toPlainString(),
                        row.getCreatedByName() == null ? "" : row.getCreatedByName(),
                        DateUtil.formatTimestamp(row.getSubmittedAt())));
            }
            CsvUtil.write(response, "declarations-export.csv", headers, csvRows);
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/search/declarations.action");
        }
    }

    private DeclarationFilter buildFilter(HttpServletRequest request) {
        DeclarationFilter filter = new DeclarationFilter();
        filter.setKeyword(WebUtil.trim(request, "keyword"));
        filter.setDeclarationTypeId(WebUtil.optionalLong(request, "typeId"));
        filter.setStatus(WebUtil.trim(request, "status"));
        filter.setPeriodFrom(DateUtil.parseDate(WebUtil.trim(request, "periodFrom")));
        filter.setPeriodTo(DateUtil.parseDate(WebUtil.trim(request, "periodTo")));
        filter.setSubmittedFrom(DateUtil.parseDate(WebUtil.trim(request, "submittedFrom")));
        filter.setSubmittedTo(DateUtil.parseDate(WebUtil.trim(request, "submittedTo")));
        filter.setReviewerId(WebUtil.optionalLong(request, "reviewerId"));
        String sort = WebUtil.trim(request, "sort");
        filter.setSort(sort.isEmpty() ? "newest" : sort);
        return filter;
    }

    private String buildFilterQuery(HttpServletRequest request) {
        return WebUtil.filterQuery(request, "keyword", "typeId", "status", "periodFrom",
                "periodTo", "submittedFrom", "submittedTo", "reviewerId", "sort");
    }

    private List<User> listReviewers() throws SQLException {
        List<Role> roles = userService.listRoles();
        Long reviewerRoleId = null;
        for (Role role : roles) {
            if ("REVIEWER".equals(role.getRoleCode())) {
                reviewerRoleId = role.getId();
            }
        }
        if (reviewerRoleId == null) {
            return List.of();
        }
        com.example.tax.dto.UserFilter filter = new com.example.tax.dto.UserFilter();
        filter.setRoleId(reviewerRoleId);
        filter.setStatus("ACTIVE");
        return userService.list(filter, 1, 100).getItems();
    }
}
