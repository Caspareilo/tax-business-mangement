package com.example.tax.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeclarationForm {

    private Long id;
    private Long taxpayerId;
    private Long declarationTypeId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Map<String, String> formValues = new LinkedHashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaxpayerId() {
        return taxpayerId;
    }

    public void setTaxpayerId(Long taxpayerId) {
        this.taxpayerId = taxpayerId;
    }

    public Long getDeclarationTypeId() {
        return declarationTypeId;
    }

    public void setDeclarationTypeId(Long declarationTypeId) {
        this.declarationTypeId = declarationTypeId;
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

    public Map<String, String> getFormValues() {
        return formValues;
    }

    public void setFormValues(Map<String, String> formValues) {
        this.formValues = formValues;
    }
}
