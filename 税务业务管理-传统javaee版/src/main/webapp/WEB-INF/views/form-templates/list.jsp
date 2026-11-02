<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="表单模板"/>
<c:set var="activeMenu" value="templates"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/form-templates.action"/>

<div class="page-header">
    <div>
        <h1 class="page-title">表单模板</h1>
        <p class="page-subtitle">每个申报类型最多保留一个已发布模板；发布新版本会自动停用同类型的旧版本，历史申报仍按其版本快照显示。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-primary" href="${ctx}/form-templates/edit.action">新建模板</a>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">筛选条件</h2>
    </div>
    <form class="filter-form" method="get" action="${ctx}/form-templates.action">
        <div class="filter-field">
            <label class="form-label" for="keyword">关键字</label>
            <input class="form-control" id="keyword" name="keyword" type="text" maxlength="100"
                   placeholder="模板编码 / 名称" value="<c:out value='${keyword}'/>">
        </div>
        <div class="filter-field">
            <label class="form-label" for="typeId">申报类型</label>
            <select class="form-select" id="typeId" name="typeId">
                <option value="">全部类型</option>
                <c:forEach var="type" items="${types}">
                    <option value="${type.id}" ${typeIdFilter == type.id ? 'selected' : ''}>
                        <c:out value="${type.typeName}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="status">状态</label>
            <select class="form-select" id="status" name="status">
                <option value="">全部状态</option>
                <option value="DRAFT" ${statusFilter == 'DRAFT' ? 'selected' : ''}>草稿</option>
                <option value="PUBLISHED" ${statusFilter == 'PUBLISHED' ? 'selected' : ''}>已发布</option>
                <option value="DISABLED" ${statusFilter == 'DISABLED' ? 'selected' : ''}>已停用</option>
            </select>
        </div>
        <div class="filter-actions">
            <button class="btn btn-primary" type="submit">查询</button>
            <a class="btn btn-outline-secondary" href="${ctx}/form-templates.action">重置</a>
        </div>
    </form>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">模板版本</h2>
        <span class="panel-link">共 ${pageData.total} 条</span>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>模板编码</th>
                <th>模板名称</th>
                <th>申报类型</th>
                <th>版本</th>
                <th>状态</th>
                <th>创建人</th>
                <th>发布时间</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${pageData.items}">
                <tr>
                    <td class="mono"><c:out value="${row.templateCode}"/></td>
                    <td><c:out value="${row.templateName}"/></td>
                    <td><c:out value="${row.declarationTypeName}"/></td>
                    <td>V${row.versionNo}</td>
                    <td>
                        <span class="status-badge ${row.published ? 'status-success' : (row.draft ? 'status-warning' : 'status-muted')}">
                            <c:out value="${row.statusLabel}"/>
                        </span>
                    </td>
                    <td><c:out value="${row.createdByName}"/></td>
                    <td><fmt:formatDate value="${row.publishedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td class="text-end table-actions">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/form-templates/preview.action?id=${row.id}">预览</a>
                        <c:if test="${row.draft}">
                            <a class="btn btn-sm btn-outline-secondary"
                               href="${ctx}/form-templates/edit.action?id=${row.id}">编辑</a>
                            <form class="inline-form" method="post" action="${ctx}/form-templates/publish.action"
                                  data-confirm="发布后该模板不可再修改，并会停用同类型的旧发布版本。确认发布？">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="id" value="${row.id}">
                                <button class="btn btn-sm btn-primary" type="submit">发布</button>
                            </form>
                        </c:if>
                        <c:if test="${row.published}">
                            <form class="inline-form" method="post" action="${ctx}/form-templates/disable.action"
                                  data-confirm="停用后该类型将没有可用模板。确认停用？">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="id" value="${row.id}">
                                <button class="btn btn-sm btn-outline-secondary" type="submit">停用</button>
                            </form>
                        </c:if>
                        <c:if test="${not row.draft}">
                            <form class="inline-form" method="post" action="${ctx}/form-templates/new-version.action"
                                  data-confirm="将基于该版本创建新的草稿版本。继续？">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="id" value="${row.id}">
                                <button class="btn btn-sm btn-outline-secondary" type="submit">新建版本</button>
                            </form>
                        </c:if>
                        <a class="btn btn-sm btn-link"
                           href="${ctx}/form-templates/versions.action?code=${row.templateCode}">版本历史</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="8" class="empty-cell">没有符合条件的表单模板。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
