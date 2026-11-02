package com.example.tax.common.enums;

public enum DeclarationStatus {
    DRAFT,
    PENDING_REVIEW,
    RETURNED,
    APPROVED,
    WITHDRAWN;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (DeclarationStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
