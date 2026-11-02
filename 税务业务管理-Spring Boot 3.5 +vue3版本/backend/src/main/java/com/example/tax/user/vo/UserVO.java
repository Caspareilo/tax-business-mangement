package com.example.tax.user.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
        Long id,
        String username,
        String displayName,
        String status,
        String remark,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        List<RoleRef> roles
) {
    public record RoleRef(Long id, String roleCode, String roleName) {
    }
}
