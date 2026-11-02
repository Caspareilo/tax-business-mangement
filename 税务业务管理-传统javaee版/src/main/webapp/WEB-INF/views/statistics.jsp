<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="统计分析"/>
<c:set var="activeMenu" value="statistics"/>
<%@ include file="common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">统计分析</h1>
        <p class="page-subtitle">按当前账号可见数据范围统计。通过率 = 审核通过 ÷（审核通过 + 已退回）。</p>
    </div>
    <div class="page-actions">
        <a class="btn ${months == 6 ? 'btn-primary' : 'btn-outline-secondary'}"
           href="${ctx}/statistics/overview.action?months=6">近 6 个月</a>
        <a class="btn ${months == 12 ? 'btn-primary' : 'btn-outline-secondary'}"
           href="${ctx}/statistics/overview.action?months=12">近 12 个月</a>
    </div>
</div>

<div class="stat-grid">
    <div class="stat-card">
        <div class="stat-label">申报总数</div>
        <div class="stat-value">${summary.totalCount}</div>
        <div class="stat-hint">含全部状态</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">审核通过</div>
        <div class="stat-value">${summary.approvedCount}</div>
        <div class="stat-hint">通过率 ${summary.approvalRateText}</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">已退回</div>
        <div class="stat-value">${summary.returnedCount}</div>
        <div class="stat-hint">需经办人修改后重提</div>
    </div>
    <div class="stat-card">
        <div class="stat-label">模拟金额合计</div>
        <div class="stat-value stat-value-amount">
            <fmt:formatNumber value="${summary.totalAmount}" type="number" minFractionDigits="2" maxFractionDigits="2" groupingUsed="true"/>
        </div>
        <div class="stat-hint">单位：元</div>
    </div>
</div>

<div class="content-grid content-grid-2">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">近 ${months} 个月申报数量</h2>
        </div>
        <div class="chart-box">
            <canvas id="trendChart" aria-label="申报数量趋势图" role="img"></canvas>
        </div>
    </section>
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">状态分布</h2>
        </div>
        <div class="chart-box">
            <canvas id="statusChart" aria-label="申报状态分布图" role="img"></canvas>
        </div>
    </section>
</div>

<div class="content-grid content-grid-2">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">按申报类型金额</h2>
        </div>
        <div class="chart-box chart-box-tall">
            <canvas id="typeChart" aria-label="按申报类型金额分布图" role="img"></canvas>
        </div>
    </section>
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">按主体类型金额</h2>
        </div>
        <div class="chart-box chart-box-tall">
            <canvas id="taxpayerTypeChart" aria-label="按主体类型金额分布图" role="img"></canvas>
        </div>
    </section>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">状态明细</h2>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th>状态</th>
                <th class="text-end">数量</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="row" items="${summary.statusDistribution}">
                <tr>
                    <td><c:out value="${row.label}"/></td>
                    <td class="text-end">${row.count}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty summary.statusDistribution}">
                <tr>
                    <td colspan="2" class="empty-cell">暂无统计数据。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</section>

<script type="application/json" id="chart-data">${chartJson}</script>
<c:set var="extraScript" value="charts.js"/>
<%@ include file="common/footer.jspf" %>
