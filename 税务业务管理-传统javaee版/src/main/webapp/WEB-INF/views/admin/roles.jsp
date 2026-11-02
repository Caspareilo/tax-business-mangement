<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="角色与权限"/>
<c:set var="activeMenu" value="roles"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">角色与权限</h1>
        <p class="page-subtitle">角色关联权限点。权限在过滤器中做粗粒度拦截，并在服务层再次校验。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-primary" href="${ctx}/admin/roles/edit.action">新增角色</a>
    </div>
</div>

<section class="panel">
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>角色编码</th>
                <th>角色名称</th>
                <th>说明</th>
                <th class="text-end">用户数</th>
                <th>权限</th>
                <th>状态</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${roles}">
                <tr>
                    <td class="mono"><c:out value="${row.roleCode}"/></td>
                    <td><c:out value="${row.roleName}"/></td>
                    <td class="cell-wrap"><c:out value="${row.description}"/></td>
                    <td class="text-end">${row.userCount}</td>
                    <td><span class="text-muted">${row.permissions.size()} 项</span></td>
                    <td>
                        <span class="status-badge ${row.active ? 'status-success' : 'status-muted'}">
                            ${row.active ? '正常' : '停用'}
                        </span>
                    </td>
                    <td class="text-end table-actions">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/admin/roles/edit.action?id=${row.id}">编辑</a>
                        <c:if test="${row.roleCode != 'ADMIN'}">
                            <form class="inline-form" method="post" action="${ctx}/admin/roles/status.action"
                                  data-confirm="${row.active ? '确认停用该角色？该角色用户将失去对应权限。' : '确认启用该角色？'}">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="id" value="${row.id}">
                                <input type="hidden" name="status" value="${row.active ? 'DISABLED' : 'ACTIVE'}">
                                <button class="btn btn-sm btn-outline-secondary" type="submit">
                                    ${row.active ? '停用' : '启用'}
                                </button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty roles}">
                <tr>
                    <td colspan="7" class="empty-cell">暂无角色数据。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</section>

<%@ include file="../common/footer.jspf" %>
