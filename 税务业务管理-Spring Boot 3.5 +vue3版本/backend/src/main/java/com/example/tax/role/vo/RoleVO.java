package com.example.tax.role.vo;

import java.time.LocalDateTime;
import java.util.List;

public record RoleVO(
        Long id,
        String roleCode,
        String roleName,
        String description,
        String status,
        long userCount,
        List<Long> permissionIds,
        LocalDateTime createdAt
) {
}
