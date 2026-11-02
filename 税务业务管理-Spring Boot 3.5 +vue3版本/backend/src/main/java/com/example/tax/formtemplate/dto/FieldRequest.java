package com.example.tax.formtemplate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record FieldRequest(
        @NotBlank(message = "请输入字段编码")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,63}$", message = "字段编码需以字母开头，仅含字母、数字、下划线")
        String fieldCode,

        @NotBlank(message = "请输入字段标签")
        @Size(max = 150, message = "字段标签不能超过 150 个字符")
        String fieldLabel,

        @NotBlank(message = "请选择字段类型")
        @Pattern(regexp = "TEXT|INTEGER|DECIMAL|DATE|RADIO|MULTI_SELECT|BOOLEAN", message = "字段类型不合法")
        String fieldType,

        Boolean required,

        @DecimalMin(value = "-9999999999999999.99", message = "最小值超出范围")
        BigDecimal minValue,

        BigDecimal maxValue,

        @Min(value = 1, message = "最大长度至少为 1")
        @Max(value = 5000, message = "最大长度不能超过 5000")
        Integer maxLength,

        @Valid
        List<FieldOptionRequest> options,

        @Size(max = 255, message = "帮助文字不能超过 255 个字符")
        String helpText,

        Integer sortOrder,

        Boolean enabled
) {
}
