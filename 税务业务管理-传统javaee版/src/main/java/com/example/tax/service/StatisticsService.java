package com.example.tax.service;

import com.example.tax.dao.DeclarationDao;
import com.example.tax.dto.MonthlyPoint;
import com.example.tax.dto.StatisticsSummary;
import com.example.tax.dto.StatusCount;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsService {

    private static final String[] STATUS_ORDER = {
            TaxDeclaration.STATUS_DRAFT,
            TaxDeclaration.STATUS_PENDING,
            TaxDeclaration.STATUS_RETURNED,
            TaxDeclaration.STATUS_APPROVED,
            TaxDeclaration.STATUS_WITHDRAWN
    };

    private final DeclarationDao declarationDao = new DeclarationDao();

    public StatisticsSummary summary(SessionUser actor, int months) throws SQLException {
        if (actor == null || (!actor.hasPermission(PermissionCodes.STATISTICS_VIEW)
                && !actor.hasPermission(PermissionCodes.DASHBOARD_VIEW))) {
            throw new ForbiddenException("无权查看统计数据");
        }
        Long creatorId = scopeCreator(actor);
        try (Connection connection = Db.getConnection()) {
            Map<String, Long> counts = declarationDao.countByStatus(connection, creatorId);
            StatisticsSummary summary = new StatisticsSummary();
            summary.setDraftCount(counts.getOrDefault(TaxDeclaration.STATUS_DRAFT, 0L));
            summary.setPendingCount(counts.getOrDefault(TaxDeclaration.STATUS_PENDING, 0L));
            summary.setReturnedCount(counts.getOrDefault(TaxDeclaration.STATUS_RETURNED, 0L));
            summary.setApprovedCount(counts.getOrDefault(TaxDeclaration.STATUS_APPROVED, 0L));
            summary.setWithdrawnCount(counts.getOrDefault(TaxDeclaration.STATUS_WITHDRAWN, 0L));
            summary.setTotalCount(counts.values().stream().mapToLong(Long::longValue).sum());
            summary.setTotalAmount(declarationDao.sumAmount(connection, creatorId));

            List<StatusCount> distribution = new ArrayList<>();
            for (String status : STATUS_ORDER) {
                distribution.add(new StatusCount(status, TaxDeclaration.statusLabel(status),
                        counts.getOrDefault(status, 0L)));
            }
            summary.setStatusDistribution(distribution);
            summary.setMonthlyTrend(fillMonths(
                    declarationDao.monthlyTrend(connection, firstDayOfWindow(months), creatorId),
                    months));
            summary.setTypeDistribution(declarationDao.amountByType(connection, creatorId));
            summary.setTaxpayerTypeDistribution(
                    declarationDao.amountByTaxpayerType(connection, creatorId));
            return summary;
        }
    }

    /**
     * Analysts/admins see all data; reviewers need the global workload; clerks and
     * viewers only see declarations they created.
     */
    private Long scopeCreator(SessionUser actor) {
        if (actor.hasPermission(PermissionCodes.STATISTICS_VIEW)
                || actor.hasPermission(PermissionCodes.REVIEW_MANAGE)) {
            return null;
        }
        return actor.getId();
    }

    public List<TaxDeclaration> recentDeclarations(int limit) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return declarationDao.recent(connection, limit);
        }
    }

    public BigDecimal totalAmount() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return declarationDao.sumAmount(connection);
        }
    }

    private List<MonthlyPoint> fillMonths(List<MonthlyPoint> rows, int months) {
        Map<String, MonthlyPoint> byMonth = new LinkedHashMap<>();
        for (MonthlyPoint point : rows) {
            byMonth.put(point.getMonth(), point);
        }
        List<MonthlyPoint> result = new ArrayList<>();
        YearMonth cursor = YearMonth.from(firstDayOfWindow(months));
        for (int i = 0; i < months; i++) {
            String key = cursor.toString();
            MonthlyPoint existing = byMonth.get(key);
            result.add(existing == null
                    ? new MonthlyPoint(key, 0, BigDecimal.ZERO) : existing);
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private LocalDate firstDayOfWindow(int months) {
        int safeMonths = Math.max(months, 1);
        return YearMonth.now().minusMonths(safeMonths - 1L).atDay(1);
    }
}
