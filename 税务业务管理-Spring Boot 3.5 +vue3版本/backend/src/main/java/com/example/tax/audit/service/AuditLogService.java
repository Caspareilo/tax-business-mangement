package com.example.tax.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.tax.audit.entity.SysOperationLog;
import com.example.tax.audit.mapper.SysOperationLogMapper;
import com.example.tax.audit.vo.AuditLogVO;
import com.example.tax.common.api.PageResult;
import com.example.tax.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AuditLogService {

    private final SysOperationLogMapper logMapper;

    public AuditLogService(SysOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public void record(String operationType, String targetType, String targetId, String result, String summary) {
        SecurityUser user = currentUser();
        record(user == null ? null : user.getId(), user == null ? null : user.getDisplayName(),
                operationType, targetType, targetId, result, summary);
    }

    public void recordLogin(String attemptedUsername, Long userId, String displayName, String result, String summary) {
        record(userId, displayName != null ? displayName : attemptedUsername,
                "LOGIN", "USER", userId == null ? null : String.valueOf(userId), result, summary);
    }

    private void record(Long operatorId, String operatorName, String operationType, String targetType,
                        String targetId, String result, String summary) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorNameSnapshot(truncate(operatorName, 100));
        log.setOperationType(truncate(operationType, 80));
        log.setTargetType(truncate(targetType, 80));
        log.setTargetId(truncate(targetId, 80));
        log.setResult(truncate(Objects.requireNonNullElse(result, "SUCCESS"), 20));
        log.setSummary(truncate(sanitize(summary), 500));
        log.setTraceId(truncate(MDC.get("traceId"), 100));
        log.setIpMasked(maskIp(currentRequest()));
        try {
            logMapper.insert(log);
        } catch (RuntimeException ignored) {
            // 审计日志失败不能影响主业务返回
        }
    }

    public PageResult<AuditLogVO> page(long page, long pageSize, Long operatorId, String operationType,
                                       String result, LocalDateTime from, LocalDateTime to, String keyword) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .eq(operatorId != null, SysOperationLog::getOperatorId, operatorId)
                .eq(operationType != null && !operationType.isBlank(), SysOperationLog::getOperationType, operationType)
                .eq(result != null && !result.isBlank(), SysOperationLog::getResult, result)
                .ge(from != null, SysOperationLog::getCreatedAt, from)
                .le(to != null, SysOperationLog::getCreatedAt, to)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysOperationLog::getSummary, keyword)
                        .or().like(SysOperationLog::getOperatorNameSnapshot, keyword))
                .orderByDesc(SysOperationLog::getCreatedAt);
        Page<SysOperationLog> resultPage = logMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(resultPage.getRecords().stream().map(AuditLogVO::from).toList(),
                resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    public PageResult<AuditLogVO> recentForCurrentUser(long pageSize) {
        SecurityUser user = currentUser();
        if (user == null) {
            return PageResult.of(java.util.List.of(), 1, pageSize, 0);
        }
        Long operatorFilter = user.getRoleCodes().contains("ADMIN") ? null : user.getId();
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<SysOperationLog>()
                .eq(operatorFilter != null, SysOperationLog::getOperatorId, operatorFilter)
                .orderByDesc(SysOperationLog::getCreatedAt);
        Page<SysOperationLog> resultPage = logMapper.selectPage(new Page<>(1, pageSize), wrapper);
        return PageResult.of(resultPage.getRecords().stream().map(AuditLogVO::from).toList(),
                resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    private SecurityUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser user) {
            return user;
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String maskIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getRemoteAddr();
        if (ip == null) {
            return null;
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".x";
        }
        int lastColon = ip.lastIndexOf(':');
        if (lastColon > 0) {
            return ip.substring(0, lastColon) + ":x";
        }
        return "masked";
    }

    private String sanitize(String summary) {
        if (summary == null) {
            return null;
        }
        return summary.replaceAll("(?i)(password|token|密码)\\s*[=:]\\s*\\S+", "$1=***");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
