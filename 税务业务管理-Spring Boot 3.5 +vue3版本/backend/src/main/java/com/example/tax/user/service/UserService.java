package com.example.tax.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.role.entity.SysRole;
import com.example.tax.role.mapper.SysRoleMapper;
import com.example.tax.role.mapper.SysUserRoleMapper;
import com.example.tax.security.CurrentUserService;
import com.example.tax.user.dto.UserCreateRequest;
import com.example.tax.user.dto.UserUpdateRequest;
import com.example.tax.user.entity.SysUser;
import com.example.tax.user.mapper.SysUserMapper;
import com.example.tax.user.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public UserService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       SysUserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder,
                       CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    public PageResult<UserVO> page(long page, long pageSize, String keyword, String status, Long roleId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getDisplayName, keyword))
                .eq(status != null && !status.isBlank(), SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreatedAt);
        if (roleId != null) {
            wrapper.inSql(SysUser::getId, "SELECT user_id FROM sys_user_role WHERE role_id = " + roleId);
        }
        Page<SysUser> resultPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<UserVO> items = resultPage.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public UserVO get(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toVO(user);
    }

    @Transactional
    public UserVO create(UserCreateRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())) > 0) {
            throw BusinessException.conflict("登录账号已存在");
        }
        List<SysRole> roles = validateRoles(request.roleIds());
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setStatus(request.status() == null ? "ACTIVE" : request.status());
        user.setRemark(request.remark());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw BusinessException.conflict("登录账号已存在");
        }
        assignRoles(user.getId(), roles);
        auditLogService.record("USER_CREATE", "USER", String.valueOf(user.getId()), "SUCCESS",
                "创建用户 " + user.getUsername());
        return get(user.getId());
    }

    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        List<SysRole> roles = validateRoles(request.roleIds());
        String status = request.status() == null ? existing.getStatus() : request.status();
        boolean self = currentUserService.isSelf(id);
        if (self && "INACTIVE".equals(status)) {
            throw BusinessException.badRequest("不能停用当前登录账号");
        }
        if (self && roles.stream().noneMatch(role -> "ADMIN".equals(role.getRoleCode()))
                && currentUserService.hasRole("ADMIN")) {
            throw BusinessException.badRequest("不能移除自己的管理员角色");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setDisplayName(request.displayName());
        update.setStatus(status);
        update.setRemark(request.remark());
        userMapper.updateById(update);
        userRoleMapper.deleteByUserId(id);
        assignRoles(id, roles);
        auditLogService.record("USER_UPDATE", "USER", String.valueOf(id), "SUCCESS",
                "更新用户 " + existing.getUsername());
        return get(id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (currentUserService.isSelf(id) && "INACTIVE".equals(status)) {
            throw BusinessException.badRequest("不能停用当前登录账号");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        auditLogService.record("USER_STATUS", "USER", String.valueOf(id), "SUCCESS",
                ("INACTIVE".equals(status) ? "停用" : "启用") + "用户 " + existing.getUsername());
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
        auditLogService.record("USER_RESET_PASSWORD", "USER", String.valueOf(id), "SUCCESS",
                "重置用户 " + existing.getUsername() + " 的密码");
    }

    @Transactional
    public void delete(Long id) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (currentUserService.isSelf(id)) {
            throw BusinessException.badRequest("不能删除当前登录账号");
        }
        if (userMapper.countDeclarationsByUser(id) > 0 || userMapper.countReviewsByUser(id) > 0) {
            throw BusinessException.conflict("该用户已有申报或审核记录，不能删除，请改用停用");
        }
        userMapper.deleteById(id);
        auditLogService.record("USER_DELETE", "USER", String.valueOf(id), "SUCCESS",
                "删除用户 " + existing.getUsername());
    }

    private List<SysRole> validateRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw BusinessException.badRequest("请至少选择一个角色");
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != Set.copyOf(roleIds).size()) {
            throw BusinessException.badRequest("包含不存在的角色");
        }
        if (roles.stream().anyMatch(role -> !"ACTIVE".equals(role.getStatus()))) {
            throw BusinessException.badRequest("不能分配已停用的角色");
        }
        return roles;
    }

    private void assignRoles(Long userId, List<SysRole> roles) {
        for (SysRole role : roles) {
            userRoleMapper.insert(userId, role.getId());
        }
    }

    private UserVO toVO(SysUser user) {
        List<UserVO.RoleRef> roles = new ArrayList<>();
        for (SysRole role : roleMapper.selectByUserId(user.getId())) {
            roles.add(new UserVO.RoleRef(role.getId(), role.getRoleCode(), role.getRoleName()));
        }
        return new UserVO(user.getId(), user.getUsername(), user.getDisplayName(), user.getStatus(),
                user.getRemark(), user.getLastLoginAt(), user.getCreatedAt(), roles);
    }
}
