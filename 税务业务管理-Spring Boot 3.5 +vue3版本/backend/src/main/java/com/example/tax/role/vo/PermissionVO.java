package com.example.tax.role.vo;

import com.example.tax.role.entity.SysPermission;

public record PermissionVO(
        Long id,
        String permissionCode,
        String permissionName,
        String permissionType,
        String routePath,
        String apiPattern,
        String httpMethod,
        Integer sortOrder
) {
    public static PermissionVO from(SysPermission permission) {
        return new PermissionVO(permission.getId(), permission.getPermissionCode(),
                permission.getPermissionName(), permission.getPermissionType(),
                permission.getRoutePath(), permission.getApiPattern(), permission.getHttpMethod(),
                permission.getSortOrder());
    }
}
