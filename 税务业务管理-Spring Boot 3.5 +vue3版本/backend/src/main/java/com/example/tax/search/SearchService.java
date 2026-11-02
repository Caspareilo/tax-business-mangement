package com.example.tax.search;

import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.declaration.dto.DeclarationQuery;
import com.example.tax.declaration.mapper.DeclarationListRow;
import com.example.tax.declaration.mapper.DeclarationMapper;
import com.example.tax.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SearchService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeclarationMapper declarationMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @Value("${app.export.max-rows:5000}")
    private int maxRows;

    public SearchService(DeclarationMapper declarationMapper, CurrentUserService currentUserService,
                         AuditLogService auditLogService) {
        this.declarationMapper = declarationMapper;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public byte[] exportCsv(DeclarationQuery query) {
        query.setScopeUserId(currentUserService.declarationScopeUserId());
        query.setSortBy(normalizeSort(query.getSortBy()));
        query.setSortOrder("asc".equalsIgnoreCase(query.getSortOrder()) ? "asc" : "desc");
        query.setLimit(maxRows + 1);
        List<DeclarationListRow> rows = declarationMapper.selectDeclarationList(query);
        if (rows.size() > maxRows) {
            throw BusinessException.badRequest("导出结果超过 " + maxRows + " 条，请缩小筛选范围");
        }
        StringBuilder builder = new StringBuilder("﻿");
        builder.append("申报编号,纳税人编号,纳税人名称,申报类型,期间开始,期间结束,状态,模拟金额,提交时间,创建人,最近审核时间\r\n");
        for (DeclarationListRow row : rows) {
            builder.append(csv(row.getDeclarationNo())).append(',')
                    .append(csv(row.getTaxpayerCode())).append(',')
                    .append(csv(row.getTaxpayerName())).append(',')
                    .append(csv(row.getDeclarationTypeName())).append(',')
                    .append(csv(row.getPeriodStart() == null ? "" : row.getPeriodStart().toString())).append(',')
                    .append(csv(row.getPeriodEnd() == null ? "" : row.getPeriodEnd().toString())).append(',')
                    .append(csv(row.getStatus())).append(',')
                    .append(csv(formatAmount(row.getDeclaredAmount()))).append(',')
                    .append(csv(row.getSubmittedAt() == null ? "" : row.getSubmittedAt().format(DATE_TIME))).append(',')
                    .append(csv(row.getCreatedByName())).append(',')
                    .append(csv(row.getLastReviewedAt() == null ? "" : row.getLastReviewedAt().format(DATE_TIME)))
                    .append("\r\n");
        }
        auditLogService.record("DECLARATION_EXPORT", "DECLARATION", null, "SUCCESS",
                "导出申报 CSV，共 " + rows.size() + " 条");
        return builder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String normalizeSort(String sortBy) {
        if (sortBy == null) {
            return "updatedAt";
        }
        return switch (sortBy) {
            case "submittedAt", "declaredAmount", "declarationNo", "createdAt", "updatedAt" -> sortBy;
            default -> "updatedAt";
        };
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
