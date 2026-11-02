package com.example.tax.model;

import java.sql.Timestamp;

public class Taxpayer {

    private long id;
    private String taxpayerCode;
    private String taxpayerName;
    private String taxpayerType;
    private String contactName;
    private String contactPhone;
    private String registrationStatus;
    private String remark;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean deleted;
    private long declarationCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaxpayerCode() {
        return taxpayerCode;
    }

    public void setTaxpayerCode(String taxpayerCode) {
        this.taxpayerCode = taxpayerCode;
    }

    public String getTaxpayerName() {
        return taxpayerName;
    }

    public void setTaxpayerName(String taxpayerName) {
        this.taxpayerName = taxpayerName;
    }

    public String getTaxpayerType() {
        return taxpayerType;
    }

    public void setTaxpayerType(String taxpayerType) {
        this.taxpayerType = taxpayerType;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public long getDeclarationCount() {
        return declarationCount;
    }

    public void setDeclarationCount(long declarationCount) {
        this.declarationCount = declarationCount;
    }

    public boolean isActive() {
        return "ACTIVE".equals(registrationStatus);
    }

    public String getTypeLabel() {
        return taxpayerTypeLabel(taxpayerType);
    }

    public String getStatusLabel() {
        return "ACTIVE".equals(registrationStatus) ? "正常" : "停用";
    }

    public static String taxpayerTypeLabel(String type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case "COMPANY":
                return "企业";
            case "INDIVIDUAL":
                return "个体工商户";
            case "INSTITUTION":
                return "事业单位";
            default:
                return type;
        }
    }
}
