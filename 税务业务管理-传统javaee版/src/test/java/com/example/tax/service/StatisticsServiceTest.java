package com.example.tax.service;

import com.example.tax.dto.StatisticsSummary;
import com.example.tax.support.DatabaseTestBase;
import com.example.tax.support.TestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatisticsServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("STAT-01 统计汇总与状态分布")
    void stat01Summary() throws Exception {
        StatisticsSummary summary = new StatisticsService().summary(analyst(), 6);

        assertEquals(12L, summary.getTotalCount());
        assertEquals(2L, summary.getDraftCount());
        assertEquals(3L, summary.getPendingCount());
        assertEquals(2L, summary.getReturnedCount());
        assertEquals(4L, summary.getApprovedCount());
        assertEquals(1L, summary.getWithdrawnCount());
        assertEquals(0, new java.math.BigDecimal("1678000.00")
                .compareTo(summary.getTotalAmount()));
        assertEquals("66.7%", summary.getApprovalRateText());
        assertEquals(5, summary.getStatusDistribution().size());
        assertEquals(6, summary.getMonthlyTrend().size());
        assertEquals(12L, summary.getMonthlyTrend().stream()
                .mapToLong(point -> point.getCount()).sum());
        assertEquals(4, summary.getTypeDistribution().size());
        assertNotNull(summary.getTaxpayerTypeDistribution());
    }

    @Test
    @DisplayName("STAT-02 经办人统计只包含本人创建的申报")
    void stat02ClerkScope() throws Exception {
        TestDatabase.execute("INSERT INTO tax_declaration (declaration_no, taxpayer_id, "
                        + "taxpayer_name_snapshot, declaration_type_id, declaration_type_snapshot, "
                        + "template_id, template_version_no, period_start, period_end, status, "
                        + "declared_amount, form_data_json, created_by) "
                        + "VALUES ('SCOPE-TEST-001', 1, '晨星数字科技（演示）有限公司', 1, "
                        + "'增值税及附加税费申报', 1, 1, ?, ?, 'DRAFT', 100.00, '{}', 1)",
                Date.valueOf("2026-07-01"), Date.valueOf("2026-07-31"));

        StatisticsService service = new StatisticsService();
        assertEquals(13L, service.summary(admin(), 6).getTotalCount());
        assertEquals(12L, service.summary(clerk(), 6).getTotalCount());
    }
}
