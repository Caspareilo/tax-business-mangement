package com.example.tax.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "请输入新密码")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "密码需为 8-64 位且同时包含字母和数字")
        String newPassword
) {
}
