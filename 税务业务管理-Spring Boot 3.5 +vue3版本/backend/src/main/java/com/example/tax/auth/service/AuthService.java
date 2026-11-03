package com.example.tax.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.auth.dto.RegisterRequest;
import com.example.tax.auth.vo.CurrentUserVO;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.role.entity.SysPermission;
import com.example.tax.role.entity.SysRole;
import com.example.tax.role.mapper.SysPermissionMapper;
import com.example.tax.role.mapper.SysRoleMapper;
import com.example.tax.role.mapper.SysUserRoleMapper;
import com.example.tax.security.CurrentUserService;
import com.example.tax.user.entity.SysUser;
import com.example.tax.user.mapper.SysUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       SysUserRoleMapper userRoleMapper,
                       SysPermissionMapper permissionMapper, PasswordEncoder passwordEncoder,
                       CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    /**
     * Public registration only grants the existing VIEWER role. No role can be
     * supplied by the client, preventing privilege escalation during sign-up.
     */
    @Transactional
    public void register(RegisterRequest request) {
        String username = request.username().trim();
        String displayName = request.displayName().trim();
        if (!request.password().equals(request.confirmPassword())) {
            throw BusinessException.badRequest("两次输入的密码不一致");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0) {
            throw BusinessException.conflict("登录账号已存在");
        }
        SysRole viewerRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "VIEWER")
                .eq(SysRole::getStatus, "ACTIVE"));
        if (viewerRole == null) {
            throw BusinessException.badRequest("系统暂未开放注册，请联系管理员");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user.setRemark("自助注册只读账号");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw BusinessException.conflict("登录账号已存在");
        }
        userRoleMapper.insert(user.getId(), viewerRole.getId());
        auditLogService.record("USER_REGISTER", "USER", String.valueOf(user.getId()),
                "SUCCESS", "自助注册只读账号 " + username);
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
