package com.example.tax.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.service.AuditLogService;
import com.example.tax.common.api.PageResult;
import com.example.tax.common.exception.BusinessException;
import com.example.tax.role.dto.PermissionAssignRequest;
import com.example.tax.role.dto.RoleRequest;
import com.example.tax.role.entity.SysPermission;
import com.example.tax.role.entity.SysRole;
import com.example.tax.role.mapper.SysPermissionMapper;
import com.example.tax.role.mapper.SysRoleMapper;
import com.example.tax.role.mapper.SysRolePermissionMapper;
import com.example.tax.role.vo.PermissionVO;
import com.example.tax.role.vo.RoleVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final AuditLogService auditLogService;

    public RoleService(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
                       SysRolePermissionMapper rolePermissionMapper, AuditLogService auditLogService) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.auditLogService = auditLogService;
    }

    public PageResult<RoleVO> page(long page, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysRole::getRoleCode, keyword)
                        .or().like(SysRole::getRoleName, keyword))
                .eq(status != null && !status.isBlank(), SysRole::getStatus, status)
                .orderByAsc(SysRole::getId);
        Page<SysRole> resultPage = roleMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<RoleVO> items = resultPage.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(items, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public List<PermissionVO> allPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSortOrder)
                        .orderByAsc(SysPermission::getId))
                .stream().map(PermissionVO::from).toList();
    }

    @Transactional
    public RoleVO create(RoleRequest request) {
        if (roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, request.roleCode())) > 0) {
            throw BusinessException.conflict("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDescription(request.description());
        role.setStatus(request.status() == null ? "ACTIVE" : request.status());
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException ex) {
            throw BusinessException.conflict("角色编码已存在");
        }
        auditLogService.record("ROLE_CREATE", "ROLE", String.valueOf(role.getId()), "SUCCESS",
                "创建角色 " + role.getRoleCode());
        return toVO(role);
    }

    @Transactional
    public RoleVO update(Long id, RoleRequest request) {
        SysRole existing = roleMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("角色不存在");
        }
        if (!existing.getRoleCode().equals(request.roleCode())) {
            throw BusinessException.badRequest("角色编码创建后不可修改");
        }
        if ("ADMIN".equals(existing.getRoleCode()) && "INACTIVE".equals(request.status())) {
            throw BusinessException.badRequest("不能停用系统管理员角色");
        }
        SysRole update = new SysRole();
        update.setId(id);
        update.setRoleName(request.roleName());
        update.setDescription(request.description());
        update.setStatus(request.status());
        roleMapper.updateById(update);
        auditLogService.record("ROLE_UPDATE", "ROLE", String.valueOf(id), "SUCCESS",
                "更新角色 " + existing.getRoleCode());
        return toVO(roleMapper.selectById(id));
    }

    @Transactional
    public void assignPermissions(Long roleId, PermissionAssignRequest request) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        List<Long> ids = request.permissionIds().stream().distinct().toList();
        if (!ids.isEmpty()) {
            List<SysPermission> permissions = permissionMapper.selectBatchIds(ids);
            if (permissions.size() != ids.size()) {
                throw BusinessException.badRequest("包含不存在的权限");
            }
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        for (Long permissionId : ids) {
            rolePermissionMapper.insert(roleId, permissionId);
        }
        auditLogService.record("ROLE_PERMISSION_UPDATE", "ROLE", String.valueOf(roleId), "SUCCESS",
                "更新角色 " + role.getRoleCode() + " 的权限，共 " + ids.size() + " 项");
    }

    private RoleVO toVO(SysRole role) {
        List<Long> permissionIds = rolePermissionMapper.selectPermissionIds(role.getId());
        return new RoleVO(role.getId(), role.getRoleCode(), role.getRoleName(), role.getDescription(),
                role.getStatus(), roleMapper.countUsers(role.getId()), permissionIds, role.getCreatedAt());
    }
}
