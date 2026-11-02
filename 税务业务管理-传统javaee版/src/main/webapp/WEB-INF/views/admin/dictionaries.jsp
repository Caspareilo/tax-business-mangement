<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="数据字典"/>
<c:set var="activeMenu" value="dictionaries"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">数据字典</h1>
        <p class="page-subtitle">维护界面下拉与提示用的字典项。停用的字典项保留历史值但不用于新选择。</p>
    </div>
</div>

<div class="content-grid content-grid-3">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">${empty editing ? '新增字典项' : '编辑字典项'}</h2>
            <c:if test="${not empty editing}">
                <a class="panel-link" href="${ctx}/admin/dictionaries.action">切换为新增</a>
            </c:if>
        </div>
        <form method="post" action="${ctx}/admin/dictionaries/save.action" novalidate>
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <c:if test="${not empty editing}">
                <input type="hidden" name="id" value="${editing.id}">
            </c:if>
            <div class="mb-3">
                <label class="form-label" for="dictType">字典类型 <span class="required-mark">*</span></label>
                <input class="form-control" id="dictType" name="dictType" type="text" maxlength="50"
                       value="<c:out value='${editing.dictType}'/>" ${empty editing ? '' : 'readonly'}>
                <div class="field-hint">示例：TAXPAYER_TYPE、DECLARATION_STATUS。</div>
            </div>
            <div class="mb-3">
                <label class="form-label" for="dictKey">字典键 <span class="required-mark">*</span></label>
                <input class="form-control" id="dictKey" name="dictKey" type="text" maxlength="50"
                       value="<c:out value='${editing.dictKey}'/>" ${empty editing ? '' : 'readonly'}>
            </div>
            <div class="mb-3">
                <label class="form-label" for="dictLabel">字典标签 <span class="required-mark">*</span></label>
                <input class="form-control" id="dictLabel" name="dictLabel" type="text" maxlength="100"
                       value="<c:out value='${editing.dictLabel}'/>">
            </div>
            <div class="mb-3">
                <label class="form-label" for="sortOrder">排序值</label>
                <input class="form-control" id="sortOrder" name="sortOrder" type="number" min="0" max="9999"
                       value="${empty editing ? 0 : editing.sortOrder}">
            </div>
            <button class="btn btn-primary" type="submit">保存字典项</button>
        </form>
    </section>

    <section class="panel panel-wide">
        <div class="panel-header">
            <h2 class="panel-title">字典项列表</h2>
        </div>
        <div class="table-responsive">
            <table class="table app-table align-middle">
                <thead>
                <tr>
                    <th>类型</th>
                    <th>键</th>
                    <th>标签</th>
                    <th class="text-end">排序</th>
                    <th>状态</th>
                    <th class="text-end">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="row" items="${items}">
                    <tr>
                        <td class="mono"><c:out value="${row.dictType}"/></td>
                        <td class="mono"><c:out value="${row.dictKey}"/></td>
                        <td><c:out value="${row.dictLabel}"/></td>
                        <td class="text-end">${row.sortOrder}</td>
                        <td>
                            <span class="status-badge ${row.enabled ? 'status-success' : 'status-muted'}">
                                ${row.enabled ? '启用' : '停用'}
                            </span>
                        </td>
                        <td class="text-end table-actions">
                            <a class="btn btn-sm btn-outline-secondary"
                               href="${ctx}/admin/dictionaries.action?editId=${row.id}">编辑</a>
                            <form class="inline-form" method="post" action="${ctx}/admin/dictionaries/status.action"
                                  data-confirm="${row.enabled ? '确认停用该字典项？' : '确认启用该字典项？'}">
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
                <c:if test="${empty items}">
                    <tr>
                        <td colspan="6" class="empty-cell">暂无字典项。</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>
</div>

<%@ include file="../common/footer.jspf" %>
