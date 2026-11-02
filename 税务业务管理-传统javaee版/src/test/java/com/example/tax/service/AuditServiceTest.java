package com.example.tax.service;

import com.example.tax.dto.AuditLogFilter;
import com.example.tax.dto.Page;
import com.example.tax.exception.BusinessException;
import com.example.tax.model.OperationLog;
import com.example.tax.support.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("AUDIT-01 登录成功与失败均写入审计且不含密码")
    void audit01LoginEvents() throws Exception {
        assertThrows(BusinessException.class, () -> login("admin", "NotThePassword1"));
        login("admin", "Admin@123");

        AuditService service = new AuditService();
        AuditLogFilter filter = new AuditLogFilter();
        filter.setOperationType("LOGIN");

        Page<OperationLog> page = service.search(filter, 1, 20);
        assertEquals(6L, page.getTotal());
        List<OperationLog> logs = page.getItems();
        assertTrue(logs.stream().anyMatch(log -> "SUCCESS".equals(log.getResult())));
        assertTrue(logs.stream().anyMatch(log -> "FAILURE".equals(log.getResult())));
        for (OperationLog log : logs) {
            String summary = log.getSummary() == null ? "" : log.getSummary();
            assertFalse(summary.contains("Admin@123"));
            assertFalse(summary.contains("NotThePassword1"));
            assertFalse(summary.toLowerCase().contains("jsessionid"));
            assertFalse(summary.toLowerCase().contains("cooki"));
        }
    }

    @Test
    @DisplayName("AUDIT-02 审计日志支持结果与操作类型筛选")
    void audit02Filters() throws Exception {
        AuditService service = new AuditService();
        AuditLogFilter filter = new AuditLogFilter();
        filter.setOperationType("LOGIN");
        filter.setResult("FAILURE");

        Page<OperationLog> page = service.search(filter, 1, 10);
        assertEquals(2L, page.getTotal());
        assertTrue(page.getItems().stream()
                .allMatch(log -> "FAILURE".equals(log.getResult())));
        assertTrue(service.operationTypes().contains("DECLARATION_APPROVE"));
    }
}
