package com.example.tax.security;

import com.example.tax.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CurrentUserService {

    public SecurityUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
        }
        return user;
    }

    public Long userId() {
        return requireUser().getId();
    }

    public String displayName() {
        return requireUser().getDisplayName();
    }

    public Set<String> roles() {
        return requireUser().getRoleCodes();
    }

    public boolean hasRole(String roleCode) {
        return roles().contains(roleCode);
    }

    public boolean hasAnyRole(String... roleCodes) {
        for (String roleCode : roleCodes) {
            if (roles().contains(roleCode)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(String permissionCode) {
        return requireUser().getPermissionCodes().contains(permissionCode);
    }

    /**
     * 返回数据范围限制：仅拥有 CLERK 且无更宽角色时，经办人只能访问本人创建的申报。
     */
    public Long declarationScopeUserId() {
        boolean wider = hasAnyRole("ADMIN", "REVIEWER", "ANALYST", "VIEWER");
        return wider ? null : userId();
    }

    public boolean isSelf(Long userId) {
        return userId != null && userId.equals(userId());
    }
}
