package com.example.tax.declaration.vo;

import com.example.tax.formtemplate.vo.FieldVO;
import com.example.tax.review.vo.ReviewVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DeclarationVO(
        Long id,
        String declarationNo,
        Long taxpayerId,
        String taxpayerCode,
        String taxpayerName,
        Long declarationTypeId,
        String declarationTypeName,
        Long templateId,
        String templateName,
        Integer templateVersionNo,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        BigDecimal declaredAmount,
        Map<String, Object> formData,
        Long createdBy,
        String createdByName,
        LocalDateTime submittedAt,
        LocalDateTime lastReviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FieldVO> fields,
        List<ReviewVO> reviews
) {
}
