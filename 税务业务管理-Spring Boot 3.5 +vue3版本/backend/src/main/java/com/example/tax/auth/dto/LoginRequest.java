package com.example.tax.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "请输入登录账号")
        @Size(max = 64, message = "账号长度不能超过 64 个字符")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(max = 64, message = "密码长度不能超过 64 个字符")
        String password
) {
}
