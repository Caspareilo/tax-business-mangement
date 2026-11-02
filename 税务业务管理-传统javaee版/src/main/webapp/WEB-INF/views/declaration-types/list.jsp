<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="申报类型"/>
<c:set var="activeMenu" value="types"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/declaration-types.action"/>

<div class="page-header">
    <div>
        <h1 class="page-title">申报类型</h1>
        <p class="page-subtitle">维护税种/申报事项类型。停用类型后不能再新建对应申报，但历史记录保留。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-primary" href="${ctx}/declaration-types/edit.action">新增申报类型</a>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">筛选条件</h2>
    </div>
    <form class="filter-form" method="get" action="${ctx}/declaration-types.action">
        <div class="filter-field">
            <label class="form-label" for="keyword">关键字</label>
            <input class="form-control" id="keyword" name="keyword" type="text" maxlength="100"
                   placeholder="类型编码 / 名称" value="<c:out value='${keyword}'/>">
        </div>
        <div class="filter-field">
            <label class="form-label" for="enabled">启用状态</label>
            <select class="form-select" id="enabled" name="enabled">
                <option value="">全部</option>
                <option value="1" ${enabledFilter == '1' ? 'selected' : ''}>已启用</option>
                <option value="0" ${enabledFilter == '0' ? 'selected' : ''}>已停用</option>
            </select>
        </div>
        <div class="filter-actions">
            <button class="btn btn-primary" type="submit">查询</button>
            <a class="btn btn-outline-secondary" href="${ctx}/declaration-types.action">重置</a>
        </div>
    </form>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">申报类型</h2>
        <span class="panel-link">共 ${pageData.total} 条</span>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>类型编码</th>
                <th>类型名称</th>
                <th>说明</th>
                <th class="text-end">申报期限（日）</th>
                <th class="text-end">已发布模板</th>
                <th>状态</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${pageData.items}">
                <tr>
                    <td class="mono"><c:out value="${row.typeCode}"/></td>
                    <td><c:out value="${row.typeName}"/></td>
                    <td class="cell-wrap"><c:out value="${row.description}"/></td>
                    <td class="text-end">${row.deadlineDay}</td>
                    <td class="text-end">${row.publishedTemplateCount}</td>
                    <td>
                        <span class="status-badge ${row.enabled ? 'status-success' : 'status-muted'}">
                            ${row.enabled ? '已启用' : '已停用'}
                        </span>
                    </td>
                    <td class="text-end table-actions">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/form-templates.action?typeId=${row.id}">模板</a>
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/declaration-types/edit.action?id=${row.id}">编辑</a>
                        <form class="inline-form" method="post" action="${ctx}/declaration-types/status.action"
                              data-confirm="${row.enabled ? '确认停用该类型？停用后不能新建对应申报。' : '确认启用该类型？'}">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="id" value="${row.id}">
                            <input type="hidden" name="enabled" value="${row.enabled ? '0' : '1'}">
                            <button class="btn btn-sm btn-outline-secondary" type="submit">
                                ${row.enabled ? '停用' : '启用'}
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="7" class="empty-cell">没有符合条件的申报类型。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
