package com.example.tax.taxpayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TaxpayerStatusRequest(
        @NotBlank(message = "请选择登记状态")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态只能是 ACTIVE 或 INACTIVE")
        String registrationStatus
) {
}
