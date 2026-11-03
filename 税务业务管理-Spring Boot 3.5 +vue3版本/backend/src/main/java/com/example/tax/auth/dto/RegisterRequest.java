package com.example.tax.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入登录账号")
        @Pattern(regexp = "^[A-Za-z0-9_.-]{3,64}$",
                message = "账号只能包含字母、数字、下划线、点或短横线，长度 3-64")
        String username,

        @NotBlank(message = "请输入姓名或显示名称")
        @Size(max = 100, message = "显示名称不能超过 100 个字符")
        String displayName,

        @NotBlank(message = "请设置密码")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "密码至少 8 位，且必须同时包含字母和数字")
        String password,

        @NotBlank(message = "请再次输入密码")
        String confirmPassword
) {
}
