package com.example.tax.role.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PermissionAssignRequest(
        @NotNull(message = "权限列表不能为空")
        List<Long> permissionIds
) {
}
