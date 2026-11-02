<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="模板版本历史"/>
<c:set var="activeMenu" value="templates"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">模板版本历史</h1>
        <p class="page-subtitle mono"><c:out value="${templateCode}"/></p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/form-templates.action">返回模板列表</a>
    </div>
</div>

<section class="panel">
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>版本</th>
                <th>模板名称</th>
                <th>状态</th>
                <th>字段数</th>
                <th>创建人</th>
                <th>创建时间</th>
                <th>发布时间</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${versions}">
                <tr>
                    <td>V${row.versionNo}</td>
                    <td><c:out value="${row.templateName}"/></td>
                    <td>
                        <span class="status-badge ${row.published ? 'status-success' : (row.draft ? 'status-warning' : 'status-muted')}">
                            <c:out value="${row.statusLabel}"/>
                        </span>
                    </td>
                    <td>${row.fields.size()}</td>
                    <td><c:out value="${row.createdByName}"/></td>
                    <td><fmt:formatDate value="${row.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td><fmt:formatDate value="${row.publishedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td class="text-end table-actions">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/form-templates/preview.action?id=${row.id}">预览</a>
                        <c:if test="${row.draft}">
                            <a class="btn btn-sm btn-outline-secondary"
                               href="${ctx}/form-templates/edit.action?id=${row.id}">编辑</a>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty versions}">
                <tr>
                    <td colspan="8" class="empty-cell">未找到该模板编码的版本记录。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</section>

<%@ include file="../common/footer.jspf" %>
