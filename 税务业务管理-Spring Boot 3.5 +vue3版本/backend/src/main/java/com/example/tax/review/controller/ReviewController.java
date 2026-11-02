package com.example.tax.review.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.declaration.vo.DeclarationVO;
import com.example.tax.review.dto.ReviewRequest;
import com.example.tax.review.service.ReviewService;
import com.example.tax.review.vo.ReviewHistoryVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews/pending")
    @PreAuthorize("hasAuthority('review:read')")
    public ApiResponse<PageResult<DeclarationVO>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long typeId) {
        return ApiResponse.ok(reviewService.pending(page, pageSize, keyword, typeId));
    }

    @GetMapping("/reviews/history")
    @PreAuthorize("hasAuthority('review:read')")
    public ApiResponse<PageResult<ReviewHistoryVO>> history(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(reviewService.history(page, pageSize, reviewerId, action, from, to, keyword));
    }

    @PostMapping("/declarations/{id}/review/approve")
    @PreAuthorize("hasAuthority('review:write')")
    public ApiResponse<Void> approve(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        reviewService.approve(id, request.comment());
        return ApiResponse.ok();
    }

    @PostMapping("/declarations/{id}/review/return")
    @PreAuthorize("hasAuthority('review:write')")
    public ApiResponse<Void> returnForRevision(@PathVariable Long id,
                                               @Valid @RequestBody ReviewRequest request) {
        reviewService.returnForRevision(id, request.comment());
        return ApiResponse.ok();
    }
}
