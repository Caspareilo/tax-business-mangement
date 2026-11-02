package com.example.tax.formtemplate.vo;

import java.math.BigDecimal;
import java.util.List;

public record FieldVO(
        Long id,
        Long templateId,
        String fieldCode,
        String fieldLabel,
        String fieldType,
        boolean required,
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer maxLength,
        List<FieldOptionVO> options,
        String helpText,
        Integer sortOrder,
        boolean enabled
) {
}
