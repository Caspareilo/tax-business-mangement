package com.example.tax.user.controller;

import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.api.PageResult;
import com.example.tax.user.dto.ResetPasswordRequest;
import com.example.tax.user.dto.UserCreateRequest;
import com.example.tax.user.dto.UserStatusRequest;
import com.example.tax.user.dto.UserUpdateRequest;
import com.example.tax.user.service.UserService;
import com.example.tax.user.vo.UserVO;
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

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roleId) {
        return ApiResponse.ok(userService.page(page, Math.min(pageSize, 100), keyword, status, roleId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<UserVO> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }
}
