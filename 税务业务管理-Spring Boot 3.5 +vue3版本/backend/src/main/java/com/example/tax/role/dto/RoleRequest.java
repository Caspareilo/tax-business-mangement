package com.example.tax.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleRequest(
        @NotBlank(message = "请输入角色编码")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "角色编码需为大写字母开头的大写字母、数字或下划线")
        String roleCode,

        @NotBlank(message = "请输入角色名称")
        @Size(max = 100, message = "角色名称不能超过 100 个字符")
        String roleName,

        @Size(max = 255, message = "描述不能超过 255 个字符")
        String description,

        @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态只能是 ACTIVE 或 INACTIVE")
        String status
) {
}
