package com.example.tax.service;

import com.example.tax.dao.DeclarationDao;
import com.example.tax.dao.FormTemplateDao;
import com.example.tax.dao.ReviewDao;
import com.example.tax.dto.Page;
import com.example.tax.exception.BusinessException;
import com.example.tax.exception.ForbiddenException;
import com.example.tax.exception.NotFoundException;
import com.example.tax.model.DeclarationReview;
import com.example.tax.model.FormField;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.security.PermissionCodes;
import com.example.tax.security.SessionUser;
import com.example.tax.util.Db;
import com.example.tax.util.JsonUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ReviewService {

    private final DeclarationDao declarationDao = new DeclarationDao();
    private final ReviewDao reviewDao = new ReviewDao();
    private final FormTemplateDao templateDao = new FormTemplateDao();
    private final AuditService auditService = new AuditService();

    public Page<TaxDeclaration> listPending(SessionUser reviewer, int page, int size)
            throws SQLException {
        requireReviewPermission(reviewer);
        int safePage = Math.max(page, 1);
        try (Connection connection = Db.getConnection()) {
            long total = declarationDao.countPending(connection);
            List<TaxDeclaration> rows = declarationDao.findPending(connection,
                    (safePage - 1) * size, size);
            return new Page<>(rows, safePage, size, total);
        }
    }

    public long countPending(SessionUser reviewer) throws SQLException {
        requireReviewPermission(reviewer);
        try (Connection connection = Db.getConnection()) {
            return declarationDao.countPending(connection);
        }
    }

    public TaxDeclaration findForReview(SessionUser reviewer, long id) throws SQLException {
        requireReviewPermission(reviewer);
        try (Connection connection = Db.getConnection()) {
            TaxDeclaration declaration = declarationDao.findById(connection, id);
            if (declaration == null) {
                throw new NotFoundException("申报记录不存在");
            }
            List<FormField> fields = templateDao.listFields(connection,
                    declaration.getTemplateId());
            declaration.setTemplateFields(fields);
            declaration.setFormValues(JsonUtil.parseFlatObject(declaration.getFormDataJson()));
            declaration.setReviews(reviewDao.listByDeclaration(connection, id));
            return declaration;
        }
    }

    public void approve(SessionUser reviewer, long id, String comment, String traceId)
            throws SQLException {
        review(reviewer, id, DeclarationReview.ACTION_APPROVE, comment, traceId);
    }

    public void returnForRevision(SessionUser reviewer, long id, String comment, String traceId)
            throws SQLException {
        if (comment == null || comment.trim().isEmpty()) {
            throw new BusinessException("退回时必须填写审核意见");
        }
        review(reviewer, id, DeclarationReview.ACTION_RETURN, comment, traceId);
    }

    private void review(SessionUser reviewer, long id, String action, String comment,
                        String traceId) throws SQLException {
        requireReviewPermission(reviewer);
        String safeComment = comment == null ? "" : comment.trim();
        if (safeComment.length() > 1000) {
            throw new BusinessException("审核意见不能超过 1000 个字符");
        }
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                TaxDeclaration declaration = declarationDao.findById(connection, id);
                if (declaration == null) {
                    throw new NotFoundException("申报记录不存在");
                }
                if (!declaration.isPending()) {
                    throw new BusinessException("该申报不在待审核状态，可能已被处理");
                }
                if (declaration.getCreatedBy() == reviewer.getId()) {
                    throw new BusinessException("经办人不能审核本人提交的申报");
                }
                String toStatus = DeclarationReview.ACTION_APPROVE.equals(action)
                        ? TaxDeclaration.STATUS_APPROVED : TaxDeclaration.STATUS_RETURNED;
                int rows = declarationDao.markReviewed(connection, id, toStatus);
                if (rows == 0) {
                    throw new BusinessException("该申报已被其他审核人处理，请刷新列表");
                }
                DeclarationReview review = new DeclarationReview();
                review.setDeclarationId(id);
                review.setReviewerId(reviewer.getId());
                review.setReviewAction(action);
                review.setReviewComment(safeComment.isEmpty() ? null : safeComment);
                review.setFromStatus(TaxDeclaration.STATUS_PENDING);
                review.setToStatus(toStatus);
                reviewDao.insert(connection, review);
                auditService.record(connection, reviewer,
                        DeclarationReview.ACTION_APPROVE.equals(action)
                                ? "DECLARATION_APPROVE" : "DECLARATION_RETURN",
                        "DECLARATION", String.valueOf(id), true,
                        (DeclarationReview.ACTION_APPROVE.equals(action) ? "审核通过 " : "退回申报 ")
                                + declaration.getDeclarationNo(), traceId);
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void requireReviewPermission(SessionUser actor) {
        if (actor == null || !actor.hasPermission(PermissionCodes.REVIEW_MANAGE)) {
            throw new ForbiddenException("无权审核申报");
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // handled by servlet error page
        }
    }
}
