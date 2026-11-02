package com.example.tax.statistics.vo;

import com.example.tax.audit.vo.AuditLogVO;
import com.example.tax.common.api.PageResult;
import com.example.tax.declaration.vo.DeclarationVO;

import java.util.List;

public record DashboardVO(
        OverviewVO overview,
        List<TrendStat> trend,
        List<StatusStat> byStatus,
        List<TypeStat> byType,
        List<TaxpayerTypeStat> byTaxpayerType,
        List<DeclarationVO> todos,
        PageResult<AuditLogVO> recentLogs
) {
}
