package com.example.tax.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FormTemplate {

    private long id;
    private String templateCode;
    private String templateName;
    private long declarationTypeId;
    private int versionNo;
    private String status;
    private long createdBy;
    private String createdByName;
    private Timestamp publishedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String declarationTypeName;
    private List<FormField> fields = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public long getDeclarationTypeId() {
        return declarationTypeId;
    }

    public void setDeclarationTypeId(long declarationTypeId) {
        this.declarationTypeId = declarationTypeId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(int versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
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

    public String getDeclarationTypeName() {
        return declarationTypeName;
    }

    public void setDeclarationTypeName(String declarationTypeName) {
        this.declarationTypeName = declarationTypeName;
    }

    public List<FormField> getFields() {
        return fields;
    }

    public void setFields(List<FormField> fields) {
        this.fields = fields;
    }

    public boolean isDraft() {
        return "DRAFT".equals(status);
    }

    public boolean isPublished() {
        return "PUBLISHED".equals(status);
    }

    public String getStatusLabel() {
        switch (status == null ? "" : status) {
            case "DRAFT":
                return "草稿";
            case "PUBLISHED":
                return "已发布";
            case "DISABLED":
                return "已停用";
            default:
                return status;
        }
    }
}
