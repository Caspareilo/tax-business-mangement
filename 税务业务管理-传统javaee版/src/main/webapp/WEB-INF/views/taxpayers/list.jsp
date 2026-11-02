<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="纳税人管理"/>
<c:set var="activeMenu" value="taxpayers"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/taxpayers.action"/>
<c:set var="canManage" value="${currentUser.hasPermission('taxpayer:manage')}"/>

<div class="page-header">
    <div>
        <h1 class="page-title">纳税人管理</h1>
        <p class="page-subtitle">维护课程演示用的纳税人档案。停用的纳税人不能再新建申报。</p>
    </div>
    <div class="page-actions">
        <c:if test="${canManage}">
            <a class="btn btn-primary" href="${ctx}/taxpayers/create.action">新增纳税人</a>
        </c:if>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">筛选条件</h2>
    </div>
    <form class="filter-form" method="get" action="${ctx}/taxpayers.action">
        <div class="filter-field">
            <label class="form-label" for="keyword">关键字</label>
            <input class="form-control" id="keyword" name="keyword" type="text" maxlength="100"
                   placeholder="编号 / 名称 / 联系人" value="<c:out value='${filter.keyword}'/>">
        </div>
        <div class="filter-field">
            <label class="form-label" for="taxpayerType">主体类型</label>
            <select class="form-select" id="taxpayerType" name="taxpayerType">
                <option value="">全部类型</option>
                <option value="COMPANY" ${filter.taxpayerType == 'COMPANY' ? 'selected' : ''}>企业</option>
                <option value="INDIVIDUAL" ${filter.taxpayerType == 'INDIVIDUAL' ? 'selected' : ''}>个体工商户</option>
                <option value="INSTITUTION" ${filter.taxpayerType == 'INSTITUTION' ? 'selected' : ''}>事业单位</option>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="status">登记状态</label>
            <select class="form-select" id="status" name="status">
                <option value="">全部状态</option>
                <option value="ACTIVE" ${filter.status == 'ACTIVE' ? 'selected' : ''}>正常</option>
                <option value="DISABLED" ${filter.status == 'DISABLED' ? 'selected' : ''}>停用</option>
            </select>
        </div>
        <div class="filter-actions">
            <button class="btn btn-primary" type="submit">查询</button>
            <a class="btn btn-outline-secondary" href="${ctx}/taxpayers.action">重置</a>
        </div>
    </form>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">纳税人档案</h2>
        <span class="panel-link">共 ${pageData.total} 条</span>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>纳税人编号</th>
                <th>纳税人名称</th>
                <th>主体类型</th>
                <th>联系人</th>
                <th>联系电话</th>
                <th>状态</th>
                <th class="text-end">申报数</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${pageData.items}">
                <tr>
                    <td class="mono"><c:out value="${row.taxpayerCode}"/></td>
                    <td><a href="${ctx}/taxpayers/detail.action?id=${row.id}"><c:out value="${row.taxpayerName}"/></a></td>
                    <td><c:out value="${row.typeLabel}"/></td>
                    <td><c:out value="${row.contactName}"/></td>
                    <td class="mono"><c:out value="${row.contactPhone}"/></td>
                    <td>
                        <span class="status-badge ${row.active ? 'status-success' : 'status-muted'}">
                            <c:out value="${row.statusLabel}"/>
                        </span>
                    </td>
                    <td class="text-end">${row.declarationCount}</td>
                    <td class="text-end table-actions">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/taxpayers/detail.action?id=${row.id}">详情</a>
                        <c:if test="${canManage}">
                            <a class="btn btn-sm btn-outline-secondary"
                               href="${ctx}/taxpayers/edit.action?id=${row.id}">编辑</a>
                            <form class="inline-form" method="post" action="${ctx}/taxpayers/status.action"
                                  data-confirm="${row.active ? '确认停用该纳税人？停用后不能新建申报。' : '确认恢复该纳税人？'}">
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
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="8" class="empty-cell">没有符合条件的纳税人。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
