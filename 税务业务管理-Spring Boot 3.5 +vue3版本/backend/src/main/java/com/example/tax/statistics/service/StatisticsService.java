package com.example.tax.statistics.service;

import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.declaration.dto.DeclarationQuery;
import com.example.tax.declaration.service.DeclarationService;
import com.example.tax.declaration.vo.DeclarationVO;
import com.example.tax.security.CurrentUserService;
import com.example.tax.statistics.mapper.StatisticsMapper;
import com.example.tax.statistics.vo.DashboardVO;
import com.example.tax.statistics.vo.OverviewStat;
import com.example.tax.statistics.vo.OverviewVO;
import com.example.tax.statistics.vo.StatusStat;
import com.example.tax.statistics.vo.TaxpayerTypeStat;
import com.example.tax.statistics.vo.TrendStat;
import com.example.tax.statistics.vo.TypeStat;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;
    private final DeclarationService declarationService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public StatisticsService(StatisticsMapper statisticsMapper, DeclarationService declarationService,
                             CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.statisticsMapper = statisticsMapper;
        this.declarationService = declarationService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public OverviewVO overview(LocalDate from, LocalDate to, Long typeId) {
        OverviewStat stat = statisticsMapper.selectOverview(
                currentUserService.declarationScopeUserId(), typeId, start(from), end(to));
        if (stat == null) {
            return OverviewVO.empty();
        }
        long approved = nz(stat.getApproved());
        long returned = nz(stat.getReturned());
        BigDecimal rate = null;
        long reviewed = approved + returned;
        if (reviewed > 0) {
            rate = BigDecimal.valueOf(approved * 100.0 / reviewed).setScale(2, RoundingMode.HALF_UP);
        }
        return new OverviewVO(nz(stat.getTotal()), nz(stat.getDraft()), nz(stat.getPending()),
                approved, returned, nz(stat.getWithdrawn()),
                stat.getTotalAmount() == null ? BigDecimal.ZERO : stat.getTotalAmount(), rate);
    }

    public List<TrendStat> trend(int months, LocalDate from, LocalDate to, Long typeId) {
        int safeMonths = Math.min(Math.max(months, 1), 24);
        LocalDateTime start;
        LocalDateTime end;
        if (from != null || to != null) {
            start = start(from == null ? LocalDate.now().minusMonths(safeMonths) : from);
            end = end(to == null ? LocalDate.now() : to);
        } else {
            start = YearMonth.now().minusMonths(safeMonths - 1L).atDay(1).atStartOfDay();
            end = LocalDateTime.now();
        }
        return statisticsMapper.selectTrend(currentUserService.declarationScopeUserId(), typeId, start, end);
    }

    public List<StatusStat> byStatus(LocalDate from, LocalDate to, Long typeId) {
        return statisticsMapper.selectByStatus(currentUserService.declarationScopeUserId(),
                typeId, start(from), end(to));
    }

    public List<TypeStat> byType(LocalDate from, LocalDate to, Long typeId) {
        return statisticsMapper.selectByType(currentUserService.declarationScopeUserId(),
                typeId, start(from), end(to));
    }

    public List<TaxpayerTypeStat> byTaxpayerType(LocalDate from, LocalDate to, Long typeId) {
        return statisticsMapper.selectByTaxpayerType(currentUserService.declarationScopeUserId(),
                typeId, start(from), end(to));
    }

    public DashboardVO dashboard() {
        OverviewVO overview = overview(null, null, null);
        List<TrendStat> trend = trend(6, null, null, null);
        List<StatusStat> byStatus = byStatus(null, null, null);
        List<TypeStat> byType = byType(null, null, null);
        List<TaxpayerTypeStat> byTaxpayerType = byTaxpayerType(null, null, null);
        List<DeclarationVO> todos = todos();
        PageResult<com.example.tax.audit.vo.AuditLogVO> recentLogs = auditLogService.recentForCurrentUser(5);
        return new DashboardVO(overview, trend, byStatus, byType, byTaxpayerType, todos, recentLogs);
    }

    private List<DeclarationVO> todos() {
        List<DeclarationVO> result = new ArrayList<>();
        if (currentUserService.hasAnyRole("REVIEWER", "ADMIN")) {
            DeclarationQuery query = new DeclarationQuery();
            query.setStatus("PENDING_REVIEW");
            query.setSortBy("submittedAt");
            query.setSortOrder("asc");
            result.addAll(declarationService.page(query, 1, 5).items());
            return result;
        }
        DeclarationQuery draftQuery = new DeclarationQuery();
        draftQuery.setStatus("DRAFT");
        draftQuery.setSortBy("updatedAt");
        draftQuery.setSortOrder("desc");
        result.addAll(declarationService.page(draftQuery, 1, 3).items());

        DeclarationQuery returnedQuery = new DeclarationQuery();
        returnedQuery.setStatus("RETURNED");
        returnedQuery.setSortBy("updatedAt");
        returnedQuery.setSortOrder("desc");
        result.addAll(declarationService.page(returnedQuery, 1, 3).items());
        return result.stream().limit(5).toList();
    }

    private long nz(Long value) {
        return value == null ? 0 : value;
    }

    private LocalDateTime start(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime end(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }
}
