<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="用户管理"/>
<c:set var="activeMenu" value="users"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/admin/users.action"/>

<div class="page-header">
    <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">维护登录账号与角色分配。停用的账号无法登录，已登录会话将在下次请求时失效。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-primary" href="${ctx}/admin/users/edit.action">新增用户</a>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">筛选条件</h2>
    </div>
    <form class="filter-form" method="get" action="${ctx}/admin/users.action">
        <div class="filter-field">
            <label class="form-label" for="keyword">关键字</label>
            <input class="form-control" id="keyword" name="keyword" type="text" maxlength="100"
                   placeholder="账号 / 显示名称" value="<c:out value='${filter.keyword}'/>">
        </div>
        <div class="filter-field">
            <label class="form-label" for="roleId">角色</label>
            <select class="form-select" id="roleId" name="roleId">
                <option value="">全部角色</option>
                <c:forEach var="role" items="${allRoles}">
                    <option value="${role.id}" ${filter.roleId == role.id ? 'selected' : ''}>
                        <c:out value="${role.roleName}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="status">状态</label>
            <select class="form-select" id="status" name="status">
                <option value="">全部状态</option>
                <option value="ACTIVE" ${filter.status == 'ACTIVE' ? 'selected' : ''}>正常</option>
                <option value="DISABLED" ${filter.status == 'DISABLED' ? 'selected' : ''}>停用</option>
            </select>
        </div>
        <div class="filter-actions">
            <button class="btn btn-primary" type="submit">查询</button>
            <a class="btn btn-outline-secondary" href="${ctx}/admin/users.action">重置</a>
        </div>
    </form>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">用户列表</h2>
        <span class="panel-link">共 ${pageData.total} 条</span>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>登录账号</th>
                <th>显示名称</th>
                <th>角色</th>
                <th>状态</th>
                <th>最近登录</th>
                <th>创建时间</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${pageData.items}">
                <tr>
                    <td class="mono"><c:out value="${row.username}"/></td>
                    <td><c:out value="${row.displayName}"/></td>
                    <td><c:out value="${row.rolesLabel}"/></td>
                    <td>
                        <span class="status-badge ${row.active ? 'status-success' : 'status-muted'}">
                            ${row.active ? '正常' : '停用'}
                        </span>
                    </td>
                    <td><fmt:formatDate value="${row.lastLoginAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td><fmt:formatDate value="${row.createdAt}" pattern="yyyy-MM-dd"/></td>
                    <td class="text-end table-actions">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/admin/users/edit.action?id=${row.id}">编辑</a>
                        <c:if test="${row.id != currentUser.id}">
                            <form class="inline-form" method="post" action="${ctx}/admin/users/status.action"
                                  data-confirm="${row.active ? '确认停用该账号？停用后无法登录。' : '确认启用该账号？'}">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="id" value="${row.id}">
                                <input type="hidden" name="status" value="${row.active ? 'DISABLED' : 'ACTIVE'}">
                                <button class="btn btn-sm btn-outline-secondary" type="submit">
                                    ${row.active ? '停用' : '启用'}
                                </button>
                            </form>
                        </c:if>
                        <form class="inline-form" method="post" action="${ctx}/admin/users/reset-password.action"
                              data-confirm="确认重置该账号密码？若密码框留空，系统会生成一次性临时密码。">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="id" value="${row.id}">
                            <input class="form-control form-control-sm reset-password-input" type="text"
                                   name="password" maxlength="100" placeholder="新密码（可留空）"
                                   autocomplete="new-password">
                            <button class="btn btn-sm btn-outline-secondary" type="submit">重置密码</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="7" class="empty-cell">没有符合条件的用户。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
