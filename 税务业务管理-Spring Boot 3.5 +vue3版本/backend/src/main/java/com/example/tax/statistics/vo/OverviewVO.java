package com.example.tax.statistics.vo;

import java.math.BigDecimal;

public record OverviewVO(
        long total,
        long draft,
        long pending,
        long approved,
        long returned,
        long withdrawn,
        BigDecimal totalAmount,
        BigDecimal approvalRate
) {
    public static OverviewVO empty() {
        return new OverviewVO(0, 0, 0, 0, 0, 0, BigDecimal.ZERO, null);
    }
}
