package com.example.tax.action;

import com.example.tax.dto.MonthlyPoint;
import com.example.tax.dto.StatisticsSummary;
import com.example.tax.dto.StatusCount;
import com.example.tax.dto.TypeAmount;
import com.example.tax.exception.AppException;
import com.example.tax.security.SessionUser;
import com.example.tax.service.StatisticsService;
import com.example.tax.util.JsonUtil;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final StatisticsService statisticsService = new StatisticsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        int months = WebUtil.intParam(request, "months", 12);
        if (months != 6 && months != 12) {
            months = 12;
        }
        try {
            StatisticsSummary summary = statisticsService.summary(user, months);
            request.setAttribute("summary", summary);
            request.setAttribute("months", months);
            request.setAttribute("chartJson", JsonUtil.safeJson(buildChart(summary)));
            forward(request, response, "statistics.jsp");
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/dashboard.action");
        }
    }

    private Map<String, Object> buildChart(StatisticsSummary summary) {
        Map<String, Object> chart = new LinkedHashMap<>();
        List<String> months = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (MonthlyPoint point : summary.getMonthlyTrend()) {
            months.add(point.getMonth());
            counts.add(point.getCount());
        }
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("labels", months);
        trend.put("counts", counts);

        List<String> statusLabels = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        for (StatusCount status : summary.getStatusDistribution()) {
            statusLabels.add(status.getLabel());
            statusCounts.add(status.getCount());
        }
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("labels", statusLabels);
        status.put("counts", statusCounts);

        List<String> typeLabels = new ArrayList<>();
        List<java.math.BigDecimal> amounts = new ArrayList<>();
        for (TypeAmount type : summary.getTypeDistribution()) {
            typeLabels.add(type.getLabel());
            amounts.add(type.getAmount());
        }
        Map<String, Object> types = new LinkedHashMap<>();
        types.put("labels", typeLabels);
        types.put("amounts", amounts);

        List<String> taxpayerLabels = new ArrayList<>();
        List<java.math.BigDecimal> taxpayerAmounts = new ArrayList<>();
        for (TypeAmount type : summary.getTaxpayerTypeDistribution()) {
            taxpayerLabels.add(type.getLabel());
            taxpayerAmounts.add(type.getAmount());
        }
        Map<String, Object> taxpayerTypes = new LinkedHashMap<>();
        taxpayerTypes.put("labels", taxpayerLabels);
        taxpayerTypes.put("amounts", taxpayerAmounts);

        chart.put("trend", trend);
        chart.put("status", status);
        chart.put("types", types);
        chart.put("taxpayerTypes", taxpayerTypes);
        return chart;
    }
}
