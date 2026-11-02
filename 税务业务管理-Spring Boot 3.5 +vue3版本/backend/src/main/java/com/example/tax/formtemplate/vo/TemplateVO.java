package com.example.tax.formtemplate.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TemplateVO(
        Long id,
        String templateCode,
        String templateName,
        Long declarationTypeId,
        String declarationTypeName,
        Integer versionNo,
        String status,
        Long createdBy,
        String createdByName,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FieldVO> fields
) {
}
