package com.example.tax.dto;

import java.math.BigDecimal;

public class TypeAmount {

    private final String label;
    private final long count;
    private final BigDecimal amount;

    public TypeAmount(String label, long count, BigDecimal amount) {
        this.label = label;
        this.count = count;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public String getLabel() {
        return label;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
