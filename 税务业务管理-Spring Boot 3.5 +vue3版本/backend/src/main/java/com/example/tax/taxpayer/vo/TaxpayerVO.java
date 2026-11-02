package com.example.tax.taxpayer.vo;

import com.example.tax.taxpayer.entity.Taxpayer;

import java.time.LocalDateTime;

public record TaxpayerVO(
        Long id,
        String taxpayerCode,
        String taxpayerName,
        String taxpayerType,
        String contactName,
        String contactPhone,
        String registrationStatus,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaxpayerVO from(Taxpayer taxpayer) {
        return new TaxpayerVO(taxpayer.getId(), taxpayer.getTaxpayerCode(), taxpayer.getTaxpayerName(),
                taxpayer.getTaxpayerType(), taxpayer.getContactName(), taxpayer.getContactPhone(),
                taxpayer.getRegistrationStatus(), taxpayer.getRemark(), taxpayer.getCreatedAt(),
                taxpayer.getUpdatedAt());
    }
}
