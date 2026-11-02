package com.example.tax.dto;

import java.math.BigDecimal;

public class MonthlyPoint {

    private final String month;
    private final long count;
    private final BigDecimal amount;

    public MonthlyPoint(String month, long count, BigDecimal amount) {
        this.month = month;
        this.count = count;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public String getMonth() {
        return month;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
