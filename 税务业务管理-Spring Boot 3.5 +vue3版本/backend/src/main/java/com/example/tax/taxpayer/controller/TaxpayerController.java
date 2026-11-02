package com.example.tax.taxpayer.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.declaration.service.DeclarationService;
import com.example.tax.declaration.vo.DeclarationVO;
import com.example.tax.taxpayer.dto.TaxpayerRequest;
import com.example.tax.taxpayer.dto.TaxpayerStatusRequest;
import com.example.tax.taxpayer.service.TaxpayerService;
import com.example.tax.taxpayer.vo.TaxpayerVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/taxpayers")
public class TaxpayerController {

    private final TaxpayerService taxpayerService;
    private final DeclarationService declarationService;

    public TaxpayerController(TaxpayerService taxpayerService, DeclarationService declarationService) {
        this.taxpayerService = taxpayerService;
        this.declarationService = declarationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('taxpayer:read')")
    public ApiResponse<PageResult<TaxpayerVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(taxpayerService.page(page, Math.min(pageSize, 100), keyword, type, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('taxpayer:read')")
    public ApiResponse<List<TaxpayerVO>> options() {
        return ApiResponse.ok(taxpayerService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('taxpayer:read')")
    public ApiResponse<TaxpayerVO> get(@PathVariable Long id) {
        return ApiResponse.ok(taxpayerService.get(id));
    }

    @GetMapping("/{id}/declarations")
    @PreAuthorize("hasAuthority('declaration:read')")
    public ApiResponse<PageResult<DeclarationVO>> declarations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        taxpayerService.get(id);
        return ApiResponse.ok(declarationService.pageByTaxpayer(id, page, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('taxpayer:write')")
    public ApiResponse<TaxpayerVO> create(@Valid @RequestBody TaxpayerRequest request) {
        return ApiResponse.ok(taxpayerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('taxpayer:write')")
    public ApiResponse<TaxpayerVO> update(@PathVariable Long id, @Valid @RequestBody TaxpayerRequest request) {
        return ApiResponse.ok(taxpayerService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('taxpayer:write')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody TaxpayerStatusRequest request) {
        taxpayerService.updateStatus(id, request.registrationStatus());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('taxpayer:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taxpayerService.delete(id);
        return ApiResponse.ok();
    }
}
