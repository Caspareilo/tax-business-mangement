<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="操作日志"/>
<c:set var="activeMenu" value="audit"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/admin/audit-logs.action"/>

<div class="page-header">
    <div>
        <h1 class="page-title">操作日志</h1>
        <p class="page-subtitle">记录关键操作的结果与追踪号，不记录密码、会话标识或完整表单敏感内容。</p>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">筛选条件</h2>
    </div>
    <form class="filter-form" method="get" action="${ctx}/admin/audit-logs.action">
        <div class="filter-field">
            <label class="form-label" for="keyword">关键字</label>
            <input class="form-control" id="keyword" name="keyword" type="text" maxlength="100"
                   placeholder="操作人 / 摘要 / 追踪号" value="<c:out value='${filter.keyword}'/>">
        </div>
        <div class="filter-field">
            <label class="form-label" for="operationType">操作类型</label>
            <select class="form-select" id="operationType" name="operationType">
                <option value="">全部类型</option>
                <c:forEach var="type" items="${operationTypes}">
                    <option value="<c:out value='${type}'/>" ${filter.operationType == type ? 'selected' : ''}>
                        <c:out value="${type}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="result">结果</label>
            <select class="form-select" id="result" name="result">
                <option value="">全部结果</option>
                <option value="SUCCESS" ${filter.result == 'SUCCESS' ? 'selected' : ''}>成功</option>
                <option value="FAILURE" ${filter.result == 'FAILURE' ? 'selected' : ''}>失败</option>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="dateFrom">开始日期</label>
            <input class="form-control" id="dateFrom" name="dateFrom" type="date" value="${filter.dateFrom}">
        </div>
        <div class="filter-field">
            <label class="form-label" for="dateTo">结束日期</label>
            <input class="form-control" id="dateTo" name="dateTo" type="date" value="${filter.dateTo}">
        </div>
        <div class="filter-actions">
            <button class="btn btn-primary" type="submit">查询</button>
            <a class="btn btn-outline-secondary" href="${ctx}/admin/audit-logs.action">重置</a>
        </div>
    </form>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">审计记录</h2>
        <span class="panel-link">共 ${pageData.total} 条</span>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>时间</th>
                <th>操作人</th>
                <th>操作类型</th>
                <th>对象</th>
                <th>摘要</th>
                <th>结果</th>
                <th>追踪号</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${pageData.items}">
                <tr>
                    <td><fmt:formatDate value="${row.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                    <td><c:out value="${row.operatorNameSnapshot}"/></td>
                    <td class="mono"><c:out value="${row.operationType}"/></td>
                    <td class="mono">
                        <c:out value="${row.targetType}"/>
                        <c:if test="${not empty row.targetId}">#<c:out value="${row.targetId}"/></c:if>
                    </td>
                    <td class="cell-wrap"><c:out value="${row.summary}"/></td>
                    <td>
                        <span class="status-badge ${row.result == 'SUCCESS' ? 'status-success' : 'status-danger'}">
                            <c:out value="${row.resultLabel}"/>
                        </span>
                    </td>
                    <td class="mono trace-cell"><c:out value="${row.traceId}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="7" class="empty-cell">没有符合条件的日志记录。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
