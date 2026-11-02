package com.example.tax.formtemplate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TemplateRequest(
        @NotBlank(message = "请输入模板编码")
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,64}$", message = "模板编码只能包含字母、数字、下划线或短横线")
        String templateCode,

        @NotBlank(message = "请输入模板名称")
        @Size(max = 150, message = "模板名称不能超过 150 个字符")
        String templateName,

        @NotNull(message = "请选择关联申报类型")
        Long declarationTypeId,

        @Valid
        List<FieldRequest> fields
) {
}
