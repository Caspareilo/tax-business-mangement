package com.example.tax.dto;

public class StatusCount {

    private final String status;
    private final String label;
    private final long count;

    public StatusCount(String status, String label, long count) {
        this.status = status;
        this.label = label;
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public String getLabel() {
        return label;
    }

    public long getCount() {
        return count;
    }
}
