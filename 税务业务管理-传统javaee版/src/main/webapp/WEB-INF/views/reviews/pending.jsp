<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="待审核申报"/>
<c:set var="activeMenu" value="reviews"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/reviews/pending.action"/>

<div class="page-header">
    <div>
        <h1 class="page-title">待审核申报</h1>
        <p class="page-subtitle">按提交时间先后展示全部待审核申报。经办人不能审核本人提交的申报。</p>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">待办列表</h2>
        <span class="panel-link">共 ${pageData.total} 条</span>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>申报编号</th>
                <th>纳税人</th>
                <th>申报类型</th>
                <th>申报期间</th>
                <th class="text-end">模拟金额（元）</th>
                <th>经办人</th>
                <th>提交时间</th>
                <th class="text-end">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${pageData.items}">
                <tr>
                    <td class="mono"><c:out value="${row.declarationNo}"/></td>
                    <td><c:out value="${row.taxpayerNameSnapshot}"/></td>
                    <td><c:out value="${row.declarationTypeSnapshot}"/></td>
                    <td class="mono">${row.periodStart} 至 ${row.periodEnd}</td>
                    <td class="text-end"><fmt:formatNumber value="${row.declaredAmount}" type="number" minFractionDigits="2" maxFractionDigits="2" groupingUsed="true"/></td>
                    <td><c:out value="${row.createdByName}"/></td>
                    <td><fmt:formatDate value="${row.submittedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td class="text-end">
                        <a class="btn btn-sm btn-primary"
                           href="${ctx}/reviews/detail.action?id=${row.id}">审核</a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="8" class="empty-cell">当前没有待审核的申报。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
