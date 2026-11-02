package com.example.tax.auth.service;

import com.example.tax.audit.service.AuditLogService;
import com.example.tax.auth.vo.CurrentUserVO;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.role.entity.SysPermission;
import com.example.tax.role.entity.SysRole;
import com.example.tax.role.mapper.SysPermissionMapper;
import com.example.tax.role.mapper.SysRoleMapper;
import com.example.tax.security.CurrentUserService;
import com.example.tax.user.entity.SysUser;
import com.example.tax.user.mapper.SysUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       SysPermissionMapper permissionMapper, PasswordEncoder passwordEncoder,
                       CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public CurrentUserVO currentUser() {
        Long userId = currentUserService.userId();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "账号已停用或不存在");
        }
        List<SysRole> roles = roleMapper.selectByUserId(userId);
        List<SysPermission> permissions = permissionMapper.selectByUserId(userId);
        List<CurrentUserVO.MenuVO> menus = permissions.stream()
                .filter(permission -> "MENU".equals(permission.getPermissionType()))
                .sorted((a, b) -> Integer.compare(
                        a.getSortOrder() == null ? 0 : a.getSortOrder(),
                        b.getSortOrder() == null ? 0 : b.getSortOrder()))
                .map(permission -> new CurrentUserVO.MenuVO(permission.getPermissionCode(),
                        permission.getPermissionName(), permission.getRoutePath(), permission.getSortOrder()))
                .toList();
        return new CurrentUserVO(user.getId(), user.getUsername(), user.getDisplayName(),
                roles.stream().map(SysRole::getRoleCode).toList(),
                permissions.stream().map(SysPermission::getPermissionCode).distinct().toList(),
                menus);
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = currentUserService.userId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw BusinessException.badRequest("当前密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw BusinessException.badRequest("新密码不能与当前密码相同");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
        auditLogService.record("PASSWORD_CHANGE", "USER", String.valueOf(userId), "SUCCESS", "用户修改密码");
    }
}
