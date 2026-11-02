package com.example.tax.declarationtype.dto;

import jakarta.validation.constraints.NotNull;

public record DeclarationTypeStatusRequest(
        @NotNull(message = "请选择启用状态")
        Boolean enabled
) {
}
