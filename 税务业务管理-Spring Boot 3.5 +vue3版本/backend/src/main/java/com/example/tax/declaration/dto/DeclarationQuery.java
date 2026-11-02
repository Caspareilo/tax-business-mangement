package com.example.tax.declaration.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class DeclarationQuery {

    private String declarationNo;
    private String keyword;
    private String taxpayerCode;
    private String taxpayerName;
    private Long declarationTypeId;
    private String status;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private LocalDateTime submittedFrom;
    private LocalDateTime submittedTo;
    private Long createdBy;
    private Long reviewerId;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Long scopeUserId;
    private String sortBy;
    private String sortOrder;
    private Integer limit;
}
