package com.example.tax.declaration.mapper;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class DeclarationListRow {

    private Long id;
    private String declarationNo;
    private Long taxpayerId;
    private String taxpayerCode;
    private String taxpayerName;
    private Long declarationTypeId;
    private String declarationTypeName;
    private Long templateId;
    private Integer templateVersionNo;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private BigDecimal declaredAmount;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime submittedAt;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
