package com.example.tax.service;

import com.example.tax.exception.BusinessException;
import com.example.tax.model.DeclarationReview;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.security.SessionUser;
import com.example.tax.support.DatabaseTestBase;
import com.example.tax.support.TestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("REVIEW-01 审核通过写入审核记录并更新状态")
    void review01Approve() throws Exception {
        SessionUser reviewer = reviewer();
        ReviewService service = new ReviewService();

        service.approve(reviewer, 2L, "资料齐全", "test-trace");

        TaxDeclaration declaration = service.findForReview(reviewer, 2L);
        assertEquals(TaxDeclaration.STATUS_APPROVED, declaration.getStatus());
        assertEquals(1, declaration.getReviews().size());
        DeclarationReview review = declaration.getReviews().get(0);
        assertEquals(DeclarationReview.ACTION_APPROVE, review.getReviewAction());
        assertEquals("李审核", review.getReviewerName());
        assertEquals(TaxDeclaration.STATUS_PENDING, review.getFromStatus());
        assertEquals(TaxDeclaration.STATUS_APPROVED, review.getToStatus());
    }

    @Test
    @DisplayName("REVIEW-02 退回必须填写审核意见")
    void review02ReturnRequiresComment() throws Exception {
        ReviewService service = new ReviewService();
        SessionUser reviewer = reviewer();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.returnForRevision(reviewer, 2L, "   ", "test-trace"));
        assertEquals("退回时必须填写审核意见", exception.getMessage());
        assertEquals(TaxDeclaration.STATUS_PENDING,
                service.findForReview(reviewer, 2L).getStatus());
    }

    @Test
    @DisplayName("REVIEW-03 经办人不能审核本人申报")
    void review03SelfReviewBlocked() throws Exception {
        TestDatabase.execute(
                "INSERT INTO tax_declaration (id, declaration_no, taxpayer_id, "
                        + "taxpayer_name_snapshot, declaration_type_id, declaration_type_snapshot, "
                        + "template_id, template_version_no, period_start, period_end, status, "
                        + "declared_amount, form_data_json, created_by, submitted_at, version, "
                        + "created_at, updated_at, deleted) VALUES (50, 'TD-SELF-000050', 6, "
                        + "'远山智能制造（演示）有限公司', 1, '增值税及附加税费申报', 1, 1, "
                        + "'2026-04-01', '2026-04-30', 'PENDING_REVIEW', 1000.00, '{}', 3, "
                        + "CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> new ReviewService().approve(reviewer(), 50L, "自审", "test-trace"));
        assertEquals("经办人不能审核本人提交的申报", exception.getMessage());
    }

    @Test
    @DisplayName("REVIEW-04 已处理申报不能被重复审核")
    void review04StaleReviewRejected() throws Exception {
        ReviewService service = new ReviewService();
        SessionUser reviewer = reviewer();

        service.approve(reviewer, 2L, "第一次通过", "test-trace");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.approve(reviewer, 2L, "重复通过", "test-trace"));
        assertTrue(exception.getMessage().contains("不在待审核状态"));
        assertEquals(1L, TestDatabase.count(
                "SELECT COUNT(*) FROM declaration_review WHERE declaration_id = 2"));
    }
}
