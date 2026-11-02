package com.example.tax.review.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.common.enums.DeclarationStatus;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.declaration.dto.DeclarationQuery;
import com.example.tax.declaration.entity.TaxDeclaration;
import com.example.tax.declaration.mapper.DeclarationMapper;
import com.example.tax.declaration.service.DeclarationService;
import com.example.tax.declaration.vo.DeclarationVO;
import com.example.tax.review.entity.DeclarationReview;
import com.example.tax.review.mapper.ReviewMapper;
import com.example.tax.review.vo.ReviewHistoryVO;
import com.example.tax.review.vo.ReviewVO;
import com.example.tax.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final DeclarationMapper declarationMapper;
    private final DeclarationService declarationService;
    private final ReviewMapper reviewMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public ReviewService(DeclarationMapper declarationMapper, DeclarationService declarationService,
                         ReviewMapper reviewMapper, CurrentUserService currentUserService,
                         AuditLogService auditLogService) {
        this.declarationMapper = declarationMapper;
        this.declarationService = declarationService;
        this.reviewMapper = reviewMapper;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public PageResult<DeclarationVO> pending(long page, long pageSize, String keyword, Long typeId) {
        DeclarationQuery query = new DeclarationQuery();
        query.setStatus(DeclarationStatus.PENDING_REVIEW.name());
        query.setKeyword(keyword);
        query.setDeclarationTypeId(typeId);
        query.setSortBy("submittedAt");
        query.setSortOrder("asc");
        return declarationService.page(query, page, pageSize);
    }

    public List<ReviewVO> listByDeclaration(Long declarationId) {
        TaxDeclaration declaration = declarationService.requireDeclaration(declarationId);
        declarationService.assertCanAccess(declaration);
        return reviewMapper.selectByDeclaration(declarationId);
    }

    public PageResult<ReviewHistoryVO> history(long page, long pageSize, Long reviewerId,
                                               String action, LocalDateTime from, LocalDateTime to,
                                               String keyword) {
        Long filterReviewer = currentUserService.hasRole("ADMIN") ? reviewerId : currentUserService.userId();
        IPage<ReviewHistoryVO> resultPage = reviewMapper.selectHistoryPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                filterReviewer, action, from, to, keyword);
        return PageResult.of(resultPage.getRecords(), resultPage.getCurrent(), resultPage.getSize(),
                resultPage.getTotal());
    }

    @Transactional
    public void approve(Long declarationId, String comment) {
        decide(declarationId, true, comment);
    }

    @Transactional
    public void returnForRevision(Long declarationId, String comment) {
        decide(declarationId, false, comment);
    }

    private void decide(Long declarationId, boolean approve, String comment) {
        TaxDeclaration declaration = declarationService.requireDeclaration(declarationId);
        if (!DeclarationStatus.PENDING_REVIEW.name().equals(declaration.getStatus())) {
            throw BusinessException.conflict("只有待审核状态的申报可以审核");
        }
        if (declaration.getCreatedBy().equals(currentUserService.userId())) {
            throw BusinessException.forbidden("不能审核本人提交的申报");
        }
        if (!approve && (comment == null || comment.isBlank())) {
            throw BusinessException.badRequest("退回修改时必须填写退回原因");
        }
        String targetStatus = approve ? DeclarationStatus.APPROVED.name() : DeclarationStatus.RETURNED.name();
        int rows = declarationMapper.updateStatusConditionalWithReview(declarationId,
                DeclarationStatus.PENDING_REVIEW.name(), targetStatus, LocalDateTime.now());
        if (rows == 0) {
            throw BusinessException.conflict("该申报已被其他审核人处理，请刷新列表");
        }
        DeclarationReview review = new DeclarationReview();
        review.setDeclarationId(declarationId);
        review.setReviewerId(currentUserService.userId());
        review.setReviewAction(approve ? "APPROVE" : "RETURN");
        review.setReviewComment(comment);
        review.setFromStatus(DeclarationStatus.PENDING_REVIEW.name());
        review.setToStatus(targetStatus);
        review.setReviewedAt(LocalDateTime.now());
        reviewMapper.insert(review);

        auditLogService.record("DECLARATION_REVIEW", "DECLARATION", String.valueOf(declarationId),
                "SUCCESS", (approve ? "审核通过" : "退回修改") + "申报 " + declaration.getDeclarationNo());
    }
}
