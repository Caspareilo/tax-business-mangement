<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="个人资料"/>
<c:set var="activeMenu" value="profile"/>
<%@ include file="common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">个人资料</h1>
        <p class="page-subtitle">查看本人账号信息、角色与权限，并修改登录密码。</p>
    </div>
</div>

<div class="detail-grid">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">账号信息</h2>
            <span class="status-badge ${profile.active ? 'status-success' : 'status-muted'}">
                ${profile.active ? '正常' : '停用'}
            </span>
        </div>
        <dl class="detail-list">
            <div>
                <dt>登录账号</dt>
                <dd class="mono"><c:out value="${profile.username}"/></dd>
            </div>
            <div>
                <dt>显示名称</dt>
                <dd><c:out value="${profile.displayName}"/></dd>
            </div>
            <div>
                <dt>所属角色</dt>
                <dd><c:out value="${profile.rolesLabel}"/></dd>
            </div>
            <div>
                <dt>最近登录</dt>
                <dd><fmt:formatDate value="${profile.lastLoginAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
            <div>
                <dt>账号创建时间</dt>
                <dd><fmt:formatDate value="${profile.createdAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
        </dl>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">修改密码</h2>
        </div>
        <form method="post" action="${ctx}/profile/password.action" novalidate>
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <div class="mb-3">
                <label class="form-label" for="oldPassword">原密码 <span class="required-mark">*</span></label>
                <input class="form-control" id="oldPassword" name="oldPassword" type="password"
                       maxlength="100" autocomplete="current-password" required>
            </div>
            <div class="mb-3">
                <label class="form-label" for="newPassword">新密码 <span class="required-mark">*</span></label>
                <input class="form-control" id="newPassword" name="newPassword" type="password"
                       maxlength="100" autocomplete="new-password" required>
                <div class="field-hint">至少 8 位，且同时包含字母和数字，不能与原密码相同。</div>
            </div>
            <div class="mb-3">
                <label class="form-label" for="confirmPassword">确认新密码 <span class="required-mark">*</span></label>
                <input class="form-control" id="confirmPassword" name="confirmPassword" type="password"
                       maxlength="100" autocomplete="new-password" required>
            </div>
            <button class="btn btn-primary" type="submit">提交修改</button>
        </form>
    </section>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">当前权限点</h2>
    </div>
    <div class="permission-tags">
        <c:forEach var="permission" items="${currentUser.permissionCodes}">
            <span class="permission-tag mono"><c:out value="${permission}"/></span>
        </c:forEach>
        <c:if test="${empty currentUser.permissionCodes}">
            <span class="text-muted">暂无权限点。</span>
        </c:if>
    </div>
</section>

<%@ include file="common/footer.jspf" %>
