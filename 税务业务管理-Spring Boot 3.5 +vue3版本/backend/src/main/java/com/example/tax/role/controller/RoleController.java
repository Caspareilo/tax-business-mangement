package com.example.tax.role.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.role.dto.PermissionAssignRequest;
import com.example.tax.role.dto.RoleRequest;
import com.example.tax.role.service.RoleService;
import com.example.tax.role.vo.PermissionVO;
import com.example.tax.role.vo.RoleVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<PageResult<RoleVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(roleService.page(page, Math.min(pageSize, 100), keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:write')")
    public ApiResponse<RoleVO> create(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:write')")
    public ApiResponse<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.ok(roleService.update(id, request));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:write')")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id,
                                               @Valid @RequestBody PermissionAssignRequest request) {
        roleService.assignPermissions(id, request);
        return ApiResponse.ok();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<List<PermissionVO>> permissions() {
        return ApiResponse.ok(roleService.allPermissions());
    }
}
