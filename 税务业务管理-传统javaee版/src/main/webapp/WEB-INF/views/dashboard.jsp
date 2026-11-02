<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="分析页"/>
<c:set var="activeMenu" value="dashboard"/>
<%@ include file="common/head.jspf" %>

<c:set var="pendingPercent" value="${summary.totalCount > 0 ? (summary.pendingCount * 100 / summary.totalCount) : 0}"/>
<c:set var="approvedPercent" value="${summary.totalCount > 0 ? (summary.approvedCount * 100 / summary.totalCount) : 0}"/>
<c:set var="approvalPercent" value="${empty summary.approvalRate ? 0 : summary.approvalRate}"/>

<header class="page-header dashboard-page-header">
    <div>
        <h1 class="page-title">分析页</h1>
        <p class="page-subtitle">
            <c:out value="${currentUser.displayName}"/>，以下数据按当前账号的数据权限实时汇总，金额均为课程演示模拟金额。
        </p>
    </div>
    <div class="page-actions">
        <c:if test="${canCreate}">
            <a class="btn btn-primary" href="${ctx}/declarations/create.action">新建申报</a>
        </c:if>
        <c:if test="${canReview}">
            <a class="btn btn-outline-secondary" href="${ctx}/reviews/pending.action">进入审核</a>
        </c:if>
    </div>
</header>

<section class="dashboard-kpis" aria-label="核心业务指标">
    <article class="metric-card">
        <div class="metric-card-head">
            <span>申报业务总量</span>
            <span class="metric-info" title="当前账号可见范围内的全部申报记录数">i</span>
        </div>
        <div class="metric-value">${summary.totalCount}</div>
        <div class="metric-visual">
            <div class="metric-sparkline-box">
                <canvas class="metric-sparkline" id="metricCountChart"
                        aria-label="近六个月申报数量迷你趋势图" role="img"></canvas>
            </div>
            <div class="metric-caption">
                <span>近 6 个月</span>
                <strong>当前账号可见范围</strong>
            </div>
        </div>
    </article>

    <article class="metric-card">
        <div class="metric-card-head">
            <span>待审核业务</span>
            <span class="metric-info" title="当前可见范围内状态为待审核的申报">i</span>
        </div>
        <div class="metric-value">${summary.pendingCount}</div>
        <div class="metric-visual">
            <div class="metric-progress" role="progressbar" aria-label="待审核业务占申报总量"
                 aria-valuemin="0" aria-valuemax="100"
                 aria-valuenow="<fmt:formatNumber value='${pendingPercent}' maxFractionDigits='1'/>">
                <span style="width: <fmt:formatNumber value='${pendingPercent}' maxFractionDigits='1'/>%"></span>
            </div>
            <div class="metric-caption">
                <span>占申报总量</span>
                <strong><fmt:formatNumber value="${pendingPercent}" minFractionDigits="1" maxFractionDigits="1"/>%</strong>
            </div>
        </div>
    </article>

    <article class="metric-card">
        <div class="metric-card-head">
            <span>审核通过</span>
            <span class="metric-info" title="通过率 = 审核通过 ÷（审核通过 + 已退回）">i</span>
        </div>
        <div class="metric-value">${summary.approvedCount}</div>
        <div class="metric-visual">
            <div class="metric-progress is-success" role="progressbar" aria-label="审核通过率"
                 aria-valuemin="0" aria-valuemax="100"
                 aria-valuenow="<fmt:formatNumber value='${approvalPercent}' maxFractionDigits='1'/>">
                <span style="width: <fmt:formatNumber value='${approvalPercent}' maxFractionDigits='1'/>%"></span>
            </div>
            <div class="metric-caption">
                <span>审核结果中的通过率</span>
                <strong>${summary.approvalRateText}</strong>
            </div>
        </div>
    </article>

    <article class="metric-card">
        <div class="metric-card-head">
            <span>申报金额合计</span>
            <span class="metric-info" title="当前可见范围内申报单的模拟金额字段合计">i</span>
        </div>
        <div class="metric-value metric-value-amount"
             title="<fmt:formatNumber value='${summary.totalAmount}' minFractionDigits='2' maxFractionDigits='2' groupingUsed='true'/> 元">
            <fmt:formatNumber value="${summary.totalAmount}" type="number"
                              minFractionDigits="2" maxFractionDigits="2" groupingUsed="true"/>
            <span class="metric-unit">元</span>
        </div>
        <div class="metric-visual">
            <div class="metric-sparkline-box">
                <canvas class="metric-sparkline" id="metricAmountChart"
                        aria-label="近六个月申报金额迷你趋势图" role="img"></canvas>
            </div>
            <div class="metric-caption">
                <span>近 6 个月</span>
                <strong>模拟金额</strong>
            </div>
        </div>
    </article>
</section>

<section class="analysis-panel" aria-labelledby="trendHeading">
    <div class="analysis-head">
        <div class="analysis-tabs" role="tablist" aria-label="趋势指标">
            <button class="analysis-tab active" id="trendCountTab" type="button"
                    role="tab" aria-selected="true" aria-controls="trendChart"
                    data-trend-metric="counts">申报数量</button>
            <button class="analysis-tab" id="trendAmountTab" type="button"
                    role="tab" aria-selected="false" aria-controls="trendChart"
                    data-trend-metric="amounts">申报金额</button>
        </div>
        <div class="analysis-meta">
            <span id="trendHeading">统计区间</span>
            <span class="analysis-meta-tag">近 6 个月</span>
        </div>
    </div>
    <div class="analysis-body">
        <div class="analysis-chart">
            <div class="chart-canvas">
                <canvas id="trendChart" aria-label="近 6 个月申报趋势图" role="img"></canvas>
            </div>
        </div>
        <aside class="analysis-side" aria-label="申报类型金额排名">
            <h2 class="analysis-side-title">申报类型金额排名</h2>
            <ol class="rank-list">
                <c:forEach var="type" items="${summary.typeDistribution}" varStatus="status">
                    <li class="rank-item">
                        <span class="rank-number">${status.index + 1}</span>
                        <span class="rank-content">
                            <span class="rank-label" title="<c:out value='${type.label}'/>"><c:out value="${type.label}"/></span>
                            <span class="rank-meta">${type.count} 笔申报</span>
                        </span>
                        <span class="rank-value">
                            ¥<fmt:formatNumber value="${type.amount}" type="number"
                                               minFractionDigits="2" maxFractionDigits="2" groupingUsed="true"/>
                        </span>
                    </li>
                </c:forEach>
                <c:if test="${empty summary.typeDistribution}">
                    <li class="empty-block">暂无可统计的申报类型金额。</li>
                </c:if>
            </ol>
        </aside>
    </div>
</section>

<div class="dashboard-lower">
    <section class="panel recent-panel" aria-labelledby="recentHeading">
        <div class="panel-header">
            <h2 class="panel-title" id="recentHeading">最近申报</h2>
            <a class="panel-link" href="${ctx}/declarations.action">查看全部申报</a>
        </div>
        <div class="table-responsive">
            <table class="table app-table align-middle">
                <thead>
                <tr>
                    <th>申报编号</th>
                    <th>纳税人</th>
                    <th>申报类型</th>
                    <th>状态</th>
                    <th>提交时间</th>
                    <th class="text-end">操作</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="row" items="${recent}">
                    <tr>
                        <td class="mono"><c:out value="${row.declarationNo}"/></td>
                        <td><c:out value="${row.taxpayerNameSnapshot}"/></td>
                        <td><c:out value="${row.declarationTypeSnapshot}"/></td>
                        <td><span class="status-badge ${row.statusBadgeClass}"><c:out value="${row.statusLabel}"/></span></td>
                        <td>
                            <c:choose>
                                <c:when test="${empty row.submittedAt}">
                                    <span class="text-muted">尚未提交</span>
                                </c:when>
                                <c:otherwise>
                                    <fmt:formatDate value="${row.submittedAt}" pattern="yyyy-MM-dd HH:mm"/>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="text-end">
                            <a class="btn btn-sm btn-outline-secondary"
                               href="${ctx}/declarations/detail.action?id=${row.id}">查看</a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty recent}">
                    <tr>
                        <td colspan="6" class="empty-cell">暂无申报记录。您可以从“新建申报”开始。</td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </section>

    <section class="panel status-panel" aria-labelledby="statusHeading">
        <div class="panel-header">
            <h2 class="panel-title" id="statusHeading">申报状态分布</h2>
            <span class="analysis-meta-tag">共 ${summary.totalCount} 笔</span>
        </div>
        <div class="status-panel-body">
            <div class="status-chart">
                <canvas id="statusChart" aria-label="申报状态分布环形图" role="img"></canvas>
                <div class="donut-center" aria-hidden="true">
                    <strong>${summary.totalCount}</strong>
                    <span>申报总量</span>
                </div>
            </div>
            <div class="status-legend" aria-label="状态图例">
                <c:forEach var="item" items="${summary.statusDistribution}" varStatus="status">
                    <div class="status-legend-item">
                        <span class="status-swatch status-swatch-${status.index}" aria-hidden="true"></span>
                        <span class="status-legend-name"><c:out value="${item.label}"/></span>
                        <span class="status-legend-value">${item.count}</span>
                    </div>
                </c:forEach>
            </div>
            <div class="queue-list" aria-label="我的工作事项">
                <a class="queue-link" href="${ctx}/declarations.action?status=DRAFT">
                    <span class="queue-label">我的草稿</span>
                    <span class="queue-value">${myDrafts}</span>
                </a>
                <a class="queue-link" href="${ctx}/declarations.action?status=RETURNED">
                    <span class="queue-label">我的退回件</span>
                    <span class="queue-value">${myReturned}</span>
                </a>
                <a class="queue-link" href="${ctx}/declarations.action?status=WITHDRAWN">
                    <span class="queue-label">已撤回</span>
                    <span class="queue-value">${summary.withdrawnCount}</span>
                </a>
                <a class="queue-link" href="${ctx}/declarations.action?status=PENDING_REVIEW">
                    <span class="queue-label">待审核</span>
                    <span class="queue-value">${summary.pendingCount}</span>
                </a>
            </div>
        </div>
    </section>
</div>

<script type="application/json" id="chart-data">${chartJson}</script>
<c:set var="extraScript" value="charts.js"/>
<%@ include file="common/footer.jspf" %>
