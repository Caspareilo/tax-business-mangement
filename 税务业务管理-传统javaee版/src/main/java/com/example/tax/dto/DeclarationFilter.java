package com.example.tax.dto;

import java.time.LocalDate;

public class DeclarationFilter {

    private String keyword;
    private Long declarationTypeId;
    private String status;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private LocalDate submittedFrom;
    private LocalDate submittedTo;
    private Long createdBy;
    private Long reviewerId;
    private Long taxpayerId;
    private String sort = "newest";

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getDeclarationTypeId() {
        return declarationTypeId;
    }

    public void setDeclarationTypeId(Long declarationTypeId) {
        this.declarationTypeId = declarationTypeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public void setPeriodFrom(LocalDate periodFrom) {
        this.periodFrom = periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public void setPeriodTo(LocalDate periodTo) {
        this.periodTo = periodTo;
    }

    public LocalDate getSubmittedFrom() {
        return submittedFrom;
    }

    public void setSubmittedFrom(LocalDate submittedFrom) {
        this.submittedFrom = submittedFrom;
    }

    public LocalDate getSubmittedTo() {
        return submittedTo;
    }

    public void setSubmittedTo(LocalDate submittedTo) {
        this.submittedTo = submittedTo;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Long getTaxpayerId() {
        return taxpayerId;
    }

    public void setTaxpayerId(Long taxpayerId) {
        this.taxpayerId = taxpayerId;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
