package com.example.tax.formtemplate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FieldOptionRequest(
        @NotBlank(message = "选项标签不能为空")
        @Size(max = 100, message = "选项标签不能超过 100 个字符")
        String label,

        @NotBlank(message = "选项值不能为空")
        @Size(max = 100, message = "选项值不能超过 100 个字符")
        String value
) {
}
