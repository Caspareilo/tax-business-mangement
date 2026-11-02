<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="纳税人详情"/>
<c:set var="activeMenu" value="taxpayers"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/taxpayers/detail.action"/>
<c:set var="filterQuery" value="id=${taxpayer.id}"/>

<div class="page-header">
    <div>
        <h1 class="page-title"><c:out value="${taxpayer.taxpayerName}"/></h1>
        <p class="page-subtitle mono"><c:out value="${taxpayer.taxpayerCode}"/></p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/taxpayers.action">返回列表</a>
        <c:if test="${currentUser.hasPermission('taxpayer:manage')}">
            <a class="btn btn-primary" href="${ctx}/taxpayers/edit.action?id=${taxpayer.id}">编辑</a>
        </c:if>
    </div>
</div>

<div class="detail-grid">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">登记信息</h2>
            <span class="status-badge ${taxpayer.active ? 'status-success' : 'status-muted'}">
                <c:out value="${taxpayer.statusLabel}"/>
            </span>
        </div>
        <dl class="detail-list">
            <div>
                <dt>主体类型</dt>
                <dd><c:out value="${taxpayer.typeLabel}"/></dd>
            </div>
            <div>
                <dt>联系人</dt>
                <dd><c:out value="${taxpayer.contactName}"/></dd>
            </div>
            <div>
                <dt>联系电话</dt>
                <dd class="mono"><c:out value="${taxpayer.contactPhone}"/></dd>
            </div>
            <div>
                <dt>累计申报数</dt>
                <dd>${taxpayer.declarationCount}</dd>
            </div>
            <div>
                <dt>创建时间</dt>
                <dd><fmt:formatDate value="${taxpayer.createdAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
            <div>
                <dt>最近更新</dt>
                <dd><fmt:formatDate value="${taxpayer.updatedAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
            <div class="detail-list-wide">
                <dt>备注</dt>
                <dd><c:out value="${empty taxpayer.remark ? '无' : taxpayer.remark}"/></dd>
            </div>
        </dl>
    </section>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">该纳税人的申报记录</h2>
        <a class="panel-link" href="${ctx}/declarations/create.action">新建申报</a>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>申报编号</th>
                <th>申报类型</th>
                <th>申报期间</th>
                <th class="text-end">模拟金额（元）</th>
                <th>状态</th>
                <th>经办人</th>
                <th>提交时间</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${pageData.items}">
                <tr>
                    <td class="mono"><c:out value="${row.declarationNo}"/></td>
                    <td><c:out value="${row.declarationTypeSnapshot}"/></td>
                    <td class="mono">${row.periodStart} 至 ${row.periodEnd}</td>
                    <td class="text-end"><fmt:formatNumber value="${row.declaredAmount}" type="number" minFractionDigits="2" maxFractionDigits="2" groupingUsed="true"/></td>
                    <td><span class="status-badge ${row.statusBadgeClass}"><c:out value="${row.statusLabel}"/></span></td>
                    <td><c:out value="${row.createdByName}"/></td>
                    <td><fmt:formatDate value="${row.submittedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td class="text-end">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/declarations/detail.action?id=${row.id}">详情</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="8" class="empty-cell">该纳税人暂无可见的申报记录。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
