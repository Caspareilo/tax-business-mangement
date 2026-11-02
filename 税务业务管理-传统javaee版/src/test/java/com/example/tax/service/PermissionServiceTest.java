package com.example.tax.service;

import com.example.tax.dto.DeclarationForm;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.support.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("PERM-01 只读用户不能填报申报")
    void perm01ViewerCannotCreateDeclaration() {
        DeclarationForm form = new DeclarationForm();
        form.setTaxpayerId(1L);
        form.setDeclarationTypeId(1L);
        form.setPeriodStart(LocalDate.of(2026, 10, 1));
        form.setPeriodEnd(LocalDate.of(2026, 10, 31));

        assertThrows(ForbiddenException.class,
                () -> new DeclarationService().saveDraft(viewer(), form, "test-trace"));
    }

    @Test
    @DisplayName("PERM-02 只读用户不能进入审核列表")
    void perm02ViewerCannotReview() {
        assertThrows(ForbiddenException.class,
                () -> new ReviewService().listPending(viewer(), 1, 10));
    }

    @Test
    @DisplayName("PERM-03 系统管理员默认不具备审核权限")
    void perm03AdminHasNoReviewPermission() throws Exception {
        assertThrows(ForbiddenException.class,
                () -> new ReviewService().listPending(admin(), 1, 10));
    }

    @Test
    @DisplayName("PERM-04 经办员不能维护用户")
    void perm04ClerkCannotManageUsers() {
        assertThrows(ForbiddenException.class,
                () -> new UserService().create(clerk(), "newclerk", "新经办",
                        "NewClerk123", List.of(2L), "test-trace"));
    }
}
