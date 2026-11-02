package com.example.tax.audit.vo;

import com.example.tax.audit.entity.SysOperationLog;

import java.time.LocalDateTime;

public record AuditLogVO(
        Long id,
        Long operatorId,
        String operatorName,
        String operationType,
        String targetType,
        String targetId,
        String result,
        String summary,
        String traceId,
        LocalDateTime createdAt
) {
    public static AuditLogVO from(SysOperationLog log) {
        return new AuditLogVO(log.getId(), log.getOperatorId(), log.getOperatorNameSnapshot(),
                log.getOperationType(), log.getTargetType(), log.getTargetId(), log.getResult(),
                log.getSummary(), log.getTraceId(), log.getCreatedAt());
    }
}
