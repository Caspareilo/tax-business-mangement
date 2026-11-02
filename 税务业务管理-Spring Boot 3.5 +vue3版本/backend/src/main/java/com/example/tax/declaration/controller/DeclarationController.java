package com.example.tax.declaration.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.declaration.dto.DeclarationQuery;
import com.example.tax.declaration.dto.DeclarationSaveRequest;
import com.example.tax.declaration.service.DeclarationService;
import com.example.tax.declaration.vo.DeclarationVO;
import com.example.tax.review.service.ReviewService;
import com.example.tax.review.vo.ReviewVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/declarations")
public class DeclarationController {

    private final DeclarationService declarationService;
    private final ReviewService reviewService;

    public DeclarationController(DeclarationService declarationService, ReviewService reviewService) {
        this.declarationService = declarationService;
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('declaration:read')")
    public ApiResponse<PageResult<DeclarationVO>> page(
            @ModelAttribute DeclarationQuery query,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return ApiResponse.ok(declarationService.page(query, page, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('declaration:read')")
    public ApiResponse<DeclarationVO> get(@PathVariable Long id) {
        return ApiResponse.ok(declarationService.get(id));
    }

    @GetMapping("/{id}/reviews")
    @PreAuthorize("hasAuthority('declaration:read')")
    public ApiResponse<List<ReviewVO>> reviews(@PathVariable Long id) {
        return ApiResponse.ok(reviewService.listByDeclaration(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('declaration:write')")
    public ApiResponse<DeclarationVO> create(@Valid @RequestBody DeclarationSaveRequest request) {
        return ApiResponse.ok(declarationService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('declaration:write')")
    public ApiResponse<DeclarationVO> update(@PathVariable Long id,
                                             @Valid @RequestBody DeclarationSaveRequest request) {
        return ApiResponse.ok(declarationService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('declaration:submit')")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        declarationService.submit(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('declaration:withdraw')")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        declarationService.withdraw(id);
        return ApiResponse.ok();
    }
}
