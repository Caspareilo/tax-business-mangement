package com.example.tax.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Minimal identity kept in HttpSession. Never contains password hashes or secrets.
 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String username;
    private final String displayName;
    private final Set<String> roleCodes;
    private final Set<String> permissionCodes;

    public SessionUser(long id, String username, String displayName,
                       Set<String> roleCodes, Set<String> permissionCodes) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roleCodes = Collections.unmodifiableSet(new LinkedHashSet<>(roleCodes));
        this.permissionCodes = Collections.unmodifiableSet(new LinkedHashSet<>(permissionCodes));
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getRoleCodes() {
        return roleCodes;
    }

    public Set<String> getPermissionCodes() {
        return permissionCodes;
    }

    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }

    public boolean isAdmin() {
        return roleCodes.contains("ADMIN");
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCodes.contains(permissionCode);
    }

    public String getRoleLabel() {
        StringBuilder label = new StringBuilder();
        for (String role : roleCodes) {
            if (label.length() > 0) {
                label.append(" / ");
            }
            label.append(roleLabel(role));
        }
        return label.toString();
    }

    private String roleLabel(String role) {
        switch (role) {
            case "ADMIN":
                return "系统管理员";
            case "CLERK":
                return "业务经办人";
            case "REVIEWER":
                return "审核人员";
            case "ANALYST":
                return "统计人员";
            case "VIEWER":
                return "只读用户";
            default:
                return role;
        }
    }
}
