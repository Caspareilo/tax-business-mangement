package com.example.tax.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StatisticsSummary {

    private long totalCount;
    private long draftCount;
    private long pendingCount;
    private long approvedCount;
    private long returnedCount;
    private long withdrawnCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private List<StatusCount> statusDistribution = new ArrayList<>();
    private List<MonthlyPoint> monthlyTrend = new ArrayList<>();
    private List<TypeAmount> typeDistribution = new ArrayList<>();
    private List<TypeAmount> taxpayerTypeDistribution = new ArrayList<>();

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getDraftCount() {
        return draftCount;
    }

    public void setDraftCount(long draftCount) {
        this.draftCount = draftCount;
    }

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public long getReturnedCount() {
        return returnedCount;
    }

    public void setReturnedCount(long returnedCount) {
        this.returnedCount = returnedCount;
    }

    public long getWithdrawnCount() {
        return withdrawnCount;
    }

    public void setWithdrawnCount(long withdrawnCount) {
        this.withdrawnCount = withdrawnCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<StatusCount> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(List<StatusCount> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public List<MonthlyPoint> getMonthlyTrend() {
        return monthlyTrend;
    }

    public void setMonthlyTrend(List<MonthlyPoint> monthlyTrend) {
        this.monthlyTrend = monthlyTrend;
    }

    public List<TypeAmount> getTypeDistribution() {
        return typeDistribution;
    }

    public void setTypeDistribution(List<TypeAmount> typeDistribution) {
        this.typeDistribution = typeDistribution;
    }

    public List<TypeAmount> getTaxpayerTypeDistribution() {
        return taxpayerTypeDistribution;
    }

    public void setTaxpayerTypeDistribution(List<TypeAmount> taxpayerTypeDistribution) {
        this.taxpayerTypeDistribution = taxpayerTypeDistribution;
    }

    /** approved / (approved + returned); null when the denominator is zero. */
    public BigDecimal getApprovalRate() {
        long denominator = approvedCount + returnedCount;
        if (denominator == 0) {
            return null;
        }
        return BigDecimal.valueOf(approvedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, java.math.RoundingMode.HALF_UP);
    }

    public String getApprovalRateText() {
        BigDecimal rate = getApprovalRate();
        return rate == null ? "—" : rate.toPlainString() + "%";
    }
}
