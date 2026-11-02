package com.example.tax.declarationtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeclarationTypeRequest(
        @NotBlank(message = "请输入类型编码")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,63}$", message = "类型编码需为大写字母、数字或下划线")
        String typeCode,

        @NotBlank(message = "请输入类型名称")
        @Size(max = 100, message = "类型名称不能超过 100 个字符")
        String typeName,

        @Size(max = 500, message = "说明不能超过 500 个字符")
        String description
) {
}
