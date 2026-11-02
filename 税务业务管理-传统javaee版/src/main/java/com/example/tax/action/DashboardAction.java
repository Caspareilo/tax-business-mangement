package com.example.tax.action;

import com.example.tax.dto.DeclarationFilter;
import com.example.tax.dto.MonthlyPoint;
import com.example.tax.dto.Page;
import com.example.tax.dto.StatisticsSummary;
import com.example.tax.dto.StatusCount;
import com.example.tax.dto.TypeAmount;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.service.DeclarationService;
import com.example.tax.service.StatisticsService;
import com.example.tax.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final StatisticsService statisticsService = new StatisticsService();
    private final DeclarationService declarationService = new DeclarationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        try {
            StatisticsSummary summary = statisticsService.summary(user, 6);
            DeclarationFilter recentFilter = new DeclarationFilter();
            Page<TaxDeclaration> recent = declarationService.list(user, recentFilter, 1, 6);

            DeclarationFilter draftFilter = new DeclarationFilter();
            draftFilter.setStatus(TaxDeclaration.STATUS_DRAFT);
            DeclarationFilter returnedFilter = new DeclarationFilter();
            returnedFilter.setStatus(TaxDeclaration.STATUS_RETURNED);
            long myDrafts = declarationService.listMine(user, draftFilter, 1, 1).getTotal();
            long myReturned = declarationService.listMine(user, returnedFilter, 1, 1).getTotal();

            request.setAttribute("summary", summary);
            request.setAttribute("recent", recent.getItems());
            request.setAttribute("myDrafts", myDrafts);
            request.setAttribute("myReturned", myReturned);
            request.setAttribute("canCreate", user.hasPermission(PermissionCodes.DECLARATION_CREATE));
            request.setAttribute("canReview", user.hasPermission(PermissionCodes.REVIEW_MANAGE));
            request.setAttribute("chartJson", JsonUtil.safeJson(buildChart(summary)));
            forward(request, response, "dashboard.jsp");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }

    private Map<String, Object> buildChart(StatisticsSummary summary) {
        Map<String, Object> chart = new LinkedHashMap<>();
        List<String> months = new ArrayList<>();
        List<Long> monthCounts = new ArrayList<>();
        List<java.math.BigDecimal> monthAmounts = new ArrayList<>();
        for (MonthlyPoint point : summary.getMonthlyTrend()) {
            months.add(point.getMonth());
            monthCounts.add(point.getCount());
            monthAmounts.add(point.getAmount());
        }
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        for (StatusCount status : summary.getStatusDistribution()) {
            statusLabels.add(status.getLabel());
            statusCounts.add(status.getCount());
        }
        List<String> typeLabels = new ArrayList<>();
        List<java.math.BigDecimal> typeAmounts = new ArrayList<>();
        for (TypeAmount type : summary.getTypeDistribution()) {
            typeLabels.add(type.getLabel());
            typeAmounts.add(type.getAmount());
        }
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("labels", months);
        trend.put("counts", monthCounts);
        trend.put("amounts", monthAmounts);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("labels", statusLabels);
        status.put("counts", statusCounts);
        Map<String, Object> types = new LinkedHashMap<>();
        types.put("labels", typeLabels);
        types.put("amounts", typeAmounts);
        chart.put("trend", trend);
        chart.put("status", status);
        chart.put("types", types);
        return chart;
    }
}
