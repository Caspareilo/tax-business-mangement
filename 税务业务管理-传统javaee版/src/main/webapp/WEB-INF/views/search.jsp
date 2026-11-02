<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="综合查询"/>
<c:set var="activeMenu" value="search"/>
<%@ include file="common/head.jspf" %>
<c:set var="paginationBase" value="/search/declarations.action"/>

<div class="page-header">
    <div>
        <h1 class="page-title">综合查询</h1>
        <p class="page-subtitle">跨纳税人、申报类型、状态与时间范围检索申报记录；经办人默认只能检索本人数据。</p>
    </div>
    <div class="page-actions">
        <c:if test="${canExport}">
            <form method="post" action="${ctx}/search/export.action" target="_blank">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="keyword" value="<c:out value='${filter.keyword}'/>">
                <input type="hidden" name="typeId" value="<c:out value='${filter.declarationTypeId}'/>">
                <input type="hidden" name="status" value="<c:out value='${filter.status}'/>">
                <input type="hidden" name="periodFrom" value="<c:out value='${filter.periodFrom}'/>">
                <input type="hidden" name="periodTo" value="<c:out value='${filter.periodTo}'/>">
                <input type="hidden" name="submittedFrom" value="<c:out value='${filter.submittedFrom}'/>">
                <input type="hidden" name="submittedTo" value="<c:out value='${filter.submittedTo}'/>">
                <input type="hidden" name="reviewerId" value="<c:out value='${filter.reviewerId}'/>">
                <input type="hidden" name="sort" value="<c:out value='${filter.sort}'/>">
                <button class="btn btn-outline-secondary" type="submit">导出当前条件 CSV（最多 5000 行）</button>
            </form>
        </c:if>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">组合筛选</h2>
        <a class="panel-link" href="${ctx}/search/declarations.action">清除全部条件</a>
    </div>
    <form class="filter-form filter-form-wide" method="get" action="${ctx}/search/declarations.action">
        <div class="filter-field">
            <label class="form-label" for="keyword">关键字</label>
            <input class="form-control" id="keyword" name="keyword" type="text" maxlength="100"
                   placeholder="申报编号 / 纳税人名称 / 类型名称"
                   value="<c:out value='${filter.keyword}'/>">
        </div>
        <div class="filter-field">
            <label class="form-label" for="typeId">申报类型</label>
            <select class="form-select" id="typeId" name="typeId">
                <option value="">全部类型</option>
                <c:forEach var="type" items="${types}">
                    <option value="${type.id}" ${filter.declarationTypeId == type.id ? 'selected' : ''}>
                        <c:out value="${type.typeName}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="status">状态</label>
            <select class="form-select" id="status" name="status">
                <option value="">全部状态</option>
                <option value="DRAFT" ${filter.status == 'DRAFT' ? 'selected' : ''}>草稿</option>
                <option value="PENDING_REVIEW" ${filter.status == 'PENDING_REVIEW' ? 'selected' : ''}>待审核</option>
                <option value="RETURNED" ${filter.status == 'RETURNED' ? 'selected' : ''}>已退回</option>
                <option value="APPROVED" ${filter.status == 'APPROVED' ? 'selected' : ''}>审核通过</option>
                <option value="WITHDRAWN" ${filter.status == 'WITHDRAWN' ? 'selected' : ''}>已撤回</option>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="reviewerId">审核人</label>
            <select class="form-select" id="reviewerId" name="reviewerId">
                <option value="">全部审核人</option>
                <c:forEach var="reviewer" items="${reviewers}">
                    <option value="${reviewer.id}" ${filter.reviewerId == reviewer.id ? 'selected' : ''}>
                        <c:out value="${reviewer.displayName}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="filter-field">
            <label class="form-label" for="periodFrom">申报期间起</label>
            <input class="form-control" id="periodFrom" name="periodFrom" type="date" value="${filter.periodFrom}">
        </div>
        <div class="filter-field">
            <label class="form-label" for="periodTo">申报期间止</label>
            <input class="form-control" id="periodTo" name="periodTo" type="date" value="${filter.periodTo}">
        </div>
        <div class="filter-field">
            <label class="form-label" for="submittedFrom">提交日期起</label>
            <input class="form-control" id="submittedFrom" name="submittedFrom" type="date"
                   value="${filter.submittedFrom}">
        </div>
        <div class="filter-field">
            <label class="form-label" for="submittedTo">提交日期止</label>
            <input class="form-control" id="submittedTo" name="submittedTo" type="date"
                   value="${filter.submittedTo}">
        </div>
        <div class="filter-field">
            <label class="form-label" for="sort">排序</label>
            <select class="form-select" id="sort" name="sort">
                <option value="newest" ${filter.sort == 'newest' ? 'selected' : ''}>最新创建</option>
                <option value="oldest" ${filter.sort == 'oldest' ? 'selected' : ''}>最早创建</option>
                <option value="amountDesc" ${filter.sort == 'amountDesc' ? 'selected' : ''}>金额从高到低</option>
                <option value="amountAsc" ${filter.sort == 'amountAsc' ? 'selected' : ''}>金额从低到高</option>
                <option value="submittedDesc" ${filter.sort == 'submittedDesc' ? 'selected' : ''}>最近提交</option>
            </select>
        </div>
        <div class="filter-actions">
            <button class="btn btn-primary" type="submit">查询</button>
            <a class="btn btn-outline-secondary" href="${ctx}/search/declarations.action">重置</a>
        </div>
    </form>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">查询结果</h2>
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
                    <td><c:out value="${row.taxpayerNameSnapshot}"/></td>
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
                    <td colspan="9" class="empty-cell">没有符合条件的申报记录。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="common/pagination.jspf" %>
</section>

<%@ include file="common/footer.jspf" %>
