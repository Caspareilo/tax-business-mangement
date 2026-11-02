package com.example.tax.audit.controller;

import com.example.tax.audit.service.AuditLogService;
import com.example.tax.audit.vo.AuditLogVO;
import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<PageResult<AuditLogVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(auditLogService.page(page, Math.min(pageSize, 100), operatorId,
                operationType, result, from, to, keyword));
    }
}
