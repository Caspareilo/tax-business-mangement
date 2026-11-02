<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${isNew ? '新增用户' : '编辑用户'}"/>
<c:set var="activeMenu" value="users"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">${isNew ? '新增用户' : '编辑用户'}</h1>
        <p class="page-subtitle">角色决定账号的菜单可见范围与服务端权限校验结果。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/admin/users.action">返回列表</a>
    </div>
</div>

<form method="post" action="${ctx}/admin/users/save.action" novalidate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <c:if test="${not isNew}">
        <input type="hidden" name="id" value="${formUser.id}">
    </c:if>
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">账号信息</h2>
        </div>
        <div class="form-grid">
            <div class="form-field">
                <label class="form-label" for="username">登录账号 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.username ? 'is-invalid' : ''}"
                       id="username" name="username" type="text" maxlength="50"
                       autocomplete="off" value="<c:out value='${formUser.username}'/>"
                       ${isNew ? '' : 'readonly'}>
                <div class="field-hint">4-50 位字母、数字或下划线，创建后不可修改。</div>
                <c:if test="${not empty fieldErrors.username}">
                    <div class="field-error"><c:out value="${fieldErrors.username}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="displayName">显示名称 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.displayName ? 'is-invalid' : ''}"
                       id="displayName" name="displayName" type="text" maxlength="100"
                       value="<c:out value='${formUser.displayName}'/>">
                <c:if test="${not empty fieldErrors.displayName}">
                    <div class="field-error"><c:out value="${fieldErrors.displayName}"/></div>
                </c:if>
            </div>
            <c:if test="${isNew}">
                <div class="form-field">
                    <label class="form-label" for="password">初始密码 <span class="required-mark">*</span></label>
                    <input class="form-control ${not empty fieldErrors.password ? 'is-invalid' : ''}"
                           id="password" name="password" type="password" maxlength="100"
                           autocomplete="new-password">
                    <div class="field-hint">至少 8 位，且同时包含字母和数字。请通过安全渠道告知使用人。</div>
                    <c:if test="${not empty fieldErrors.password}">
                        <div class="field-error"><c:out value="${fieldErrors.password}"/></div>
                    </c:if>
                </div>
            </c:if>
            <c:if test="${not isNew}">
                <div class="form-field">
                    <label class="form-label" for="status">账号状态</label>
                    <select class="form-select ${not empty fieldErrors.status ? 'is-invalid' : ''}"
                            id="status" name="status">
                        <option value="ACTIVE" ${formUser.status == 'ACTIVE' ? 'selected' : ''}>正常</option>
                        <option value="DISABLED" ${formUser.status == 'DISABLED' ? 'selected' : ''}>停用</option>
                    </select>
                    <div class="field-hint">不能停用当前登录账号。</div>
                    <c:if test="${not empty fieldErrors.status}">
                        <div class="field-error"><c:out value="${fieldErrors.status}"/></div>
                    </c:if>
                </div>
            </c:if>
        </div>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">角色分配 <span class="required-mark">*</span></h2>
        </div>
        <c:if test="${not empty fieldErrors.roleIds}">
            <div class="field-error mb-2"><c:out value="${fieldErrors.roleIds}"/></div>
        </c:if>
        <div class="role-choice-grid">
            <c:forEach var="role" items="${allRoles}">
                <label class="role-choice">
                    <input class="form-check-input" type="checkbox" name="roleIds" value="${role.id}"
                           ${formUser.roleIds.contains(role.id) ? 'checked' : ''}>
                    <span>
                        <span class="role-choice-name"><c:out value="${role.roleName}"/></span>
                        <span class="role-choice-code mono"><c:out value="${role.roleCode}"/></span>
                        <span class="role-choice-desc"><c:out value="${role.description}"/></span>
                    </span>
                </label>
            </c:forEach>
        </div>
        <p class="field-hint mt-2">至少选择一个角色。管理员默认不拥有审核权限，避免“默认代审”。</p>
    </section>

    <div class="form-actions">
        <button class="btn btn-primary" type="submit">保存</button>
        <a class="btn btn-outline-secondary" href="${ctx}/admin/users.action">取消</a>
    </div>
</form>

<%@ include file="../common/footer.jspf" %>
