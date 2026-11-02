package com.example.tax.service;

import com.example.tax.dto.AuditLogFilter;
import com.example.tax.exception.BusinessException;
import com.example.tax.model.OperationLog;
import com.example.tax.security.SessionUser;
import com.example.tax.support.DatabaseTestBase;
import com.example.tax.support.TestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("AUTH-01 正确账号密码可登录并加载角色权限")
    void auth01LoginSuccess() throws Exception {
        SessionUser user = login("clerk1", "Clerk@123");

        assertEquals(2L, user.getId());
        assertEquals("张经办", user.getDisplayName());
        assertTrue(user.hasRole("CLERK"));
        assertTrue(user.hasPermission("declaration:create"));
        assertTrue(user.hasPermission("declaration:submit"));
        assertFalse(user.hasPermission("review:manage"));
        assertEquals("业务经办人", user.getRoleLabel());
    }

    @Test
    @DisplayName("AUTH-02 错误密码被拒绝且写入失败审计")
    void auth02WrongPassword() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> login("clerk1", "WrongPassword1"));
        assertEquals("账号或密码错误", exception.getMessage());

        AuditLogFilter filter = new AuditLogFilter();
        filter.setOperationType("LOGIN");
        filter.setResult("FAILURE");
        try {
            List<OperationLog> logs = new AuditService().search(filter, 1, 20).getItems();
            assertTrue(logs.stream().anyMatch(log -> log.getSummary() != null
                    && log.getSummary().contains("登录失败")));
            assertFalse(logs.stream().anyMatch(log -> log.getSummary() != null
                    && log.getSummary().contains("WrongPassword1")));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("AUTH-03 停用账号不能登录")
    void auth03DisabledUser() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> login("disabled1", "Disabled@123"));
        assertEquals("账号已停用，请联系系统管理员", exception.getMessage());
        assertEquals(0L, TestDatabase.count(
                "SELECT COUNT(*) FROM sys_operation_log WHERE operator_id = 6 "
                        + "AND operation_type = 'LOGIN' AND result = 'SUCCESS'"));
    }

    @Test
    @DisplayName("AUTH-04 登录账号不存在时返回统一错误，避免账号枚举")
    void auth04UnknownUser() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> login("nobody", "Nobody@123"));
        assertEquals("账号或密码错误", exception.getMessage());
    }
}
