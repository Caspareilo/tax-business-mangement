package com.example.tax.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(
        @NotBlank(message = "请选择状态")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态只能是 ACTIVE 或 INACTIVE")
        String status
) {
}
