package com.example.tax.declaration.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record DeclarationSaveRequest(
        @NotNull(message = "请选择纳税人")
        Long taxpayerId,

        @NotNull(message = "请选择申报类型")
        Long declarationTypeId,

        @NotNull(message = "请选择申报期间开始日期")
        LocalDate periodStart,

        @NotNull(message = "请选择申报期间结束日期")
        LocalDate periodEnd,

        @DecimalMin(value = "0.00", message = "申报金额不能为负数")
        BigDecimal declaredAmount,

        Map<String, Object> formData
) {
}
