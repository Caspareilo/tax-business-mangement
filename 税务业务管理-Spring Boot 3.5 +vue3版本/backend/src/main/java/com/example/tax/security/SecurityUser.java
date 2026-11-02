package com.example.tax.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String displayName;
    private final String status;
    private final Set<String> roleCodes;
    private final Set<String> permissionCodes;

    public SecurityUser(Long id, String username, String password, String displayName, String status,
                        Set<String> roleCodes, Set<String> permissionCodes) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.status = status;
        this.roleCodes = roleCodes == null ? Set.of() : roleCodes;
        this.permissionCodes = permissionCodes == null ? Set.of() : permissionCodes;
    }

    public Long getId() {
        return id;
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> authorities = new LinkedHashSet<>(permissionCodes);
        roleCodes.forEach(role -> authorities.add("ROLE_" + role));
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
