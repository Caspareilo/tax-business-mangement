package com.example.tax.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaxDeclaration {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING_REVIEW";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private long id;
    private String declarationNo;
    private long taxpayerId;
    private String taxpayerNameSnapshot;
    private String taxpayerCode;
    private long declarationTypeId;
    private String declarationTypeSnapshot;
    private long templateId;
    private int templateVersionNo;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private BigDecimal declaredAmount;
    private String formDataJson;
    private long createdBy;
    private String createdByName;
    private Timestamp submittedAt;
    private Timestamp lastReviewedAt;
    private int version;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean deleted;

    private List<FormField> templateFields = new ArrayList<>();
    private Map<String, String> formValues = new LinkedHashMap<>();
    private List<DeclarationReview> reviews = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDeclarationNo() {
        return declarationNo;
    }

    public void setDeclarationNo(String declarationNo) {
        this.declarationNo = declarationNo;
    }

    public long getTaxpayerId() {
        return taxpayerId;
    }

    public void setTaxpayerId(long taxpayerId) {
        this.taxpayerId = taxpayerId;
    }

    public String getTaxpayerNameSnapshot() {
        return taxpayerNameSnapshot;
    }

    public void setTaxpayerNameSnapshot(String taxpayerNameSnapshot) {
        this.taxpayerNameSnapshot = taxpayerNameSnapshot;
    }

    public String getTaxpayerCode() {
        return taxpayerCode;
    }

    public void setTaxpayerCode(String taxpayerCode) {
        this.taxpayerCode = taxpayerCode;
    }

    public long getDeclarationTypeId() {
        return declarationTypeId;
    }

    public void setDeclarationTypeId(long declarationTypeId) {
        this.declarationTypeId = declarationTypeId;
    }

    public String getDeclarationTypeSnapshot() {
        return declarationTypeSnapshot;
    }

    public void setDeclarationTypeSnapshot(String declarationTypeSnapshot) {
        this.declarationTypeSnapshot = declarationTypeSnapshot;
    }

    public long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }

    public int getTemplateVersionNo() {
        return templateVersionNo;
    }

    public void setTemplateVersionNo(int templateVersionNo) {
        this.templateVersionNo = templateVersionNo;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getDeclaredAmount() {
        return declaredAmount;
    }

    public void setDeclaredAmount(BigDecimal declaredAmount) {
        this.declaredAmount = declaredAmount;
    }

    public String getFormDataJson() {
        return formDataJson;
    }

    public void setFormDataJson(String formDataJson) {
        this.formDataJson = formDataJson;
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

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Timestamp getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Timestamp lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public List<FormField> getTemplateFields() {
        return templateFields;
    }

    public void setTemplateFields(List<FormField> templateFields) {
        this.templateFields = templateFields;
    }

    public Map<String, String> getFormValues() {
        return formValues;
    }

    public void setFormValues(Map<String, String> formValues) {
        this.formValues = formValues;
    }

    public List<DeclarationReview> getReviews() {
        return reviews;
    }

    public void setReviews(List<DeclarationReview> reviews) {
        this.reviews = reviews;
    }

    public boolean isEditable() {
        return STATUS_DRAFT.equals(status) || STATUS_RETURNED.equals(status);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public String getStatusLabel() {
        return statusLabel(status);
    }

    public String getStatusBadgeClass() {
        return statusBadgeClass(status);
    }

    public static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_DRAFT:
                return "草稿";
            case STATUS_PENDING:
                return "待审核";
            case STATUS_RETURNED:
                return "已退回";
            case STATUS_APPROVED:
                return "审核通过";
            case STATUS_WITHDRAWN:
                return "已撤回";
            default:
                return status;
        }
    }

    public static String statusBadgeClass(String status) {
        if (status == null) {
            return "status-neutral";
        }
        switch (status) {
            case STATUS_DRAFT:
                return "status-neutral";
            case STATUS_PENDING:
                return "status-warning";
            case STATUS_RETURNED:
                return "status-danger";
            case STATUS_APPROVED:
                return "status-success";
            case STATUS_WITHDRAWN:
                return "status-muted";
            default:
                return "status-neutral";
        }
    }
}
