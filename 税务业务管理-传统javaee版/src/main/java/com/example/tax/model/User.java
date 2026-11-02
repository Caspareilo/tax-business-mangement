package com.example.tax.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class User {

    private long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String status;
    private Timestamp lastLoginAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean deleted;
    private List<Role> roles = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Timestamp lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public String getRolesLabel() {
        StringBuilder label = new StringBuilder();
        for (Role role : roles) {
            if (label.length() > 0) {
                label.append("、");
            }
            label.append(role.getRoleName());
        }
        return label.toString();
    }

    public List<Long> getRoleIds() {
        List<Long> ids = new ArrayList<>();
        for (Role role : roles) {
            ids.add(role.getId());
        }
        return ids;
    }
}
