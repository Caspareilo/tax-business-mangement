package com.example.tax.auth.controller;

import com.example.tax.audit.service.AuditLogService;
import com.example.tax.auth.dto.ChangePasswordRequest;
import com.example.tax.auth.dto.LoginRequest;
import com.example.tax.auth.dto.RegisterRequest;
import com.example.tax.auth.service.AuthService;
import com.example.tax.auth.vo.CurrentUserVO;
import com.example.tax.common.api.ApiResponse;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.common.exception.ErrorCode;
import com.example.tax.security.SecurityUser;
import com.example.tax.user.entity.SysUser;
import com.example.tax.user.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final SysUserMapper userMapper;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager, AuthService authService,
                          SysUserMapper userMapper, AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ApiResponse<CurrentUserVO> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            auditLogService.recordLogin(request.username(), null, request.username(),
                    "FAILURE", "登录失败");
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                    "账号或密码错误，或账号已停用");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        SysUser update = new SysUser();
        update.setId(principal.getId());
        update.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(update);

        auditLogService.recordLogin(principal.getUsername(), principal.getId(), principal.getDisplayName(),
                "SUCCESS", "登录成功");
        return ApiResponse.ok(authService.currentUser());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        auditLogService.record("LOGOUT", "USER", null, "SUCCESS", "退出登录");
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ApiResponse.ok();
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.oldPassword(), request.newPassword());
        return ApiResponse.ok();
    }
}
