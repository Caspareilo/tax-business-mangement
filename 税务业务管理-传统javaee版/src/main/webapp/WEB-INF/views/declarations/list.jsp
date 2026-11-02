<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="我的申报"/>
<c:set var="activeMenu" value="declarations"/>
<%@ include file="../common/head.jspf" %>
<c:set var="paginationBase" value="/declarations.action"/>

<div class="page-header">
    <div>
        <h1 class="page-title">我的申报</h1>
        <p class="page-subtitle">仅展示当前账号创建的申报记录，可按关键字、状态和申报类型筛选。</p>
    </div>
    <div class="page-actions">
        <c:if test="${currentUser.hasPermission('declaration:create')}">
            <a class="btn btn-primary" href="${ctx}/declarations/create.action">新建申报</a>
        </c:if>
    </div>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">筛选条件</h2>
    </div>
    <form class="filter-form" method="get" action="${ctx}/declarations.action">
        <div class="filter-field">
            <label class="form-label" for="keyword">关键字</label>
            <input class="form-control" id="keyword" name="keyword" type="text" maxlength="100"
                   placeholder="申报编号 / 纳税人名称" value="<c:out value='${filter.keyword}'/>">
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
        <div class="filter-actions">
            <button class="btn btn-primary" type="submit">查询</button>
            <a class="btn btn-outline-secondary" href="${ctx}/declarations.action">重置</a>
        </div>
    </form>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">申报记录</h2>
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
                    <td><fmt:formatDate value="${row.submittedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td class="text-end table-actions">
                        <a class="btn btn-sm btn-outline-secondary"
                           href="${ctx}/declarations/detail.action?id=${row.id}">详情</a>
                        <c:if test="${row.createdBy == currentUser.id && row.editable}">
                            <a class="btn btn-sm btn-outline-secondary"
                               href="${ctx}/declarations/edit.action?id=${row.id}">编辑</a>
                        </c:if>
                        <c:if test="${row.createdBy == currentUser.id && row.editable}">
                            <form class="inline-form" method="post" action="${ctx}/declarations/submit.action"
                                  data-confirm="提交后将进入审核流程，期间不能编辑。确认提交？">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="id" value="${row.id}">
                                <button class="btn btn-sm btn-primary" type="submit">提交</button>
                            </form>
                        </c:if>
                        <c:if test="${row.pending && (row.createdBy == currentUser.id || currentUser.admin)}">
                            <form class="inline-form" method="post" action="${ctx}/declarations/withdraw.action"
                                  data-confirm="确认撤回该申报？撤回后可继续编辑并重新提交。">
                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                <input type="hidden" name="id" value="${row.id}">
                                <button class="btn btn-sm btn-outline-secondary" type="submit">撤回</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr>
                    <td colspan="8" class="empty-cell">没有符合条件的申报记录。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <%@ include file="../common/pagination.jspf" %>
</section>

<%@ include file="../common/footer.jspf" %>
