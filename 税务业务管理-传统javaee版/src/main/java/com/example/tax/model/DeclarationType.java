package com.example.tax.model;

import java.sql.Timestamp;

public class DeclarationType {

    private long id;
    private String typeCode;
    private String typeName;
    private String description;
    private boolean enabled;
    private Integer deadlineDay;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int publishedTemplateCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getDeadlineDay() {
        return deadlineDay;
    }

    public void setDeadlineDay(Integer deadlineDay) {
        this.deadlineDay = deadlineDay;
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

    public int getPublishedTemplateCount() {
        return publishedTemplateCount;
    }

    public void setPublishedTemplateCount(int publishedTemplateCount) {
        this.publishedTemplateCount = publishedTemplateCount;
    }
}
