<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${isNew ? '新增角色' : '编辑角色'}"/>
<c:set var="activeMenu" value="roles"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">${isNew ? '新增角色' : '编辑角色'}</h1>
        <p class="page-subtitle">角色编码创建后不可修改；权限调整只影响后续请求，已登录用户需重新登录才会刷新菜单。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/admin/roles.action">返回列表</a>
    </div>
</div>

<form method="post" action="${ctx}/admin/roles/save.action" novalidate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <c:if test="${not isNew}">
        <input type="hidden" name="id" value="${formRole.id}">
    </c:if>
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">角色信息</h2>
        </div>
        <div class="form-grid">
            <div class="form-field">
                <label class="form-label" for="roleCode">角色编码 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.roleCode ? 'is-invalid' : ''}"
                       id="roleCode" name="roleCode" type="text" maxlength="40"
                       value="<c:out value='${formRole.roleCode}'/>" ${isNew ? '' : 'readonly'}>
                <div class="field-hint">大写字母、数字与下划线，示例：QUALITY_INSPECTOR。</div>
                <c:if test="${not empty fieldErrors.roleCode}">
                    <div class="field-error"><c:out value="${fieldErrors.roleCode}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="roleName">角色名称 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.roleName ? 'is-invalid' : ''}"
                       id="roleName" name="roleName" type="text" maxlength="100"
                       value="<c:out value='${formRole.roleName}'/>">
                <c:if test="${not empty fieldErrors.roleName}">
                    <div class="field-error"><c:out value="${fieldErrors.roleName}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="status">状态</label>
                <select class="form-select ${not empty fieldErrors.status ? 'is-invalid' : ''}"
                        id="status" name="status">
                    <option value="ACTIVE" ${formRole.status == 'ACTIVE' ? 'selected' : ''}>正常</option>
                    <option value="DISABLED" ${formRole.status == 'DISABLED' ? 'selected' : ''}>停用</option>
                </select>
                <div class="field-hint">系统管理员角色不能停用。</div>
                <c:if test="${not empty fieldErrors.status}">
                    <div class="field-error"><c:out value="${fieldErrors.status}"/></div>
                </c:if>
            </div>
            <div class="form-field form-field-wide">
                <label class="form-label" for="description">说明</label>
                <textarea class="form-control ${not empty fieldErrors.description ? 'is-invalid' : ''}"
                          id="description" name="description" rows="3"
                          maxlength="255"><c:out value="${formRole.description}"/></textarea>
                <c:if test="${not empty fieldErrors.description}">
                    <div class="field-error"><c:out value="${fieldErrors.description}"/></div>
                </c:if>
            </div>
        </div>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">权限点</h2>
        </div>
        <div class="permission-grid">
            <c:forEach var="permission" items="${permissions}">
                <label class="permission-choice">
                    <input class="form-check-input" type="checkbox" name="permissionIds"
                           value="${permission.id}"
                           ${formRole.permissionIds.contains(permission.id) ? 'checked' : ''}>
                    <span>
                        <span class="permission-choice-name"><c:out value="${permission.permissionName}"/></span>
                        <span class="permission-choice-code mono"><c:out value="${permission.permissionCode}"/></span>
                        <c:if test="${not empty permission.groupName}">
                            <span class="permission-choice-group"><c:out value="${permission.groupName}"/></span>
                        </c:if>
                    </span>
                </label>
            </c:forEach>
        </div>
        <p class="field-hint mt-2">管理员默认不勾选“审核申报”，确保管理员不会在无审核角色的情况下代审。</p>
    </section>

    <div class="form-actions">
        <button class="btn btn-primary" type="submit">保存</button>
        <a class="btn btn-outline-secondary" href="${ctx}/admin/roles.action">取消</a>
    </div>
</form>

<%@ include file="../common/footer.jspf" %>
