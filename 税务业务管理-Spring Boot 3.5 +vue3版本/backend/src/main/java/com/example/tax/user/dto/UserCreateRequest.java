package com.example.tax.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(
        @NotBlank(message = "请输入登录账号")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,63}$", message = "账号需以字母开头，3-64 位字母、数字或下划线")
        String username,

        @NotBlank(message = "请输入初始密码")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "密码需为 8-64 位且同时包含字母和数字")
        String password,

        @NotBlank(message = "请输入显示名称")
        @Size(max = 100, message = "显示名称不能超过 100 个字符")
        String displayName,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态只能是 ACTIVE 或 INACTIVE")
        String status,

        @Size(max = 255, message = "备注不能超过 255 个字符")
        String remark,

        @NotEmpty(message = "请至少选择一个角色")
        List<Long> roleIds
) {
}
