package com.example.tax.declarationtype.vo;

import com.example.tax.declarationtype.entity.DeclarationType;

import java.time.LocalDateTime;

public record DeclarationTypeVO(
        Long id,
        String typeCode,
        String typeName,
        String description,
        boolean enabled,
        Long defaultTemplateId,
        String defaultTemplateName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DeclarationTypeVO from(DeclarationType type, String defaultTemplateName) {
        return new DeclarationTypeVO(type.getId(), type.getTypeCode(), type.getTypeName(),
                type.getDescription(), type.getEnabled() != null && type.getEnabled() == 1,
                type.getDefaultTemplateId(), defaultTemplateName, type.getCreatedAt(), type.getUpdatedAt());
    }
}
