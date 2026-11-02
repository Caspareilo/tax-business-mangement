package com.example.tax.declarationtype.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.declarationtype.dto.DeclarationTypeRequest;
import com.example.tax.declarationtype.dto.DeclarationTypeStatusRequest;
import com.example.tax.declarationtype.service.DeclarationTypeService;
import com.example.tax.declarationtype.vo.DeclarationTypeVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/declaration-types")
public class DeclarationTypeController {

    private final DeclarationTypeService typeService;

    public DeclarationTypeController(DeclarationTypeService typeService) {
        this.typeService = typeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('declaration-type:read')")
    public ApiResponse<PageResult<DeclarationTypeVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(typeService.page(page, Math.min(pageSize, 100), keyword, enabled));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('declaration-type:read')")
    public ApiResponse<List<DeclarationTypeVO>> options() {
        return ApiResponse.ok(typeService.enabledOptions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('declaration-type:read')")
    public ApiResponse<DeclarationTypeVO> get(@PathVariable Long id) {
        return ApiResponse.ok(typeService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('declaration-type:write')")
    public ApiResponse<DeclarationTypeVO> create(@Valid @RequestBody DeclarationTypeRequest request) {
        return ApiResponse.ok(typeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('declaration-type:write')")
    public ApiResponse<DeclarationTypeVO> update(@PathVariable Long id,
                                                 @Valid @RequestBody DeclarationTypeRequest request) {
        return ApiResponse.ok(typeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('declaration-type:write')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody DeclarationTypeStatusRequest request) {
        typeService.updateStatus(id, request.enabled());
        return ApiResponse.ok();
    }
}
