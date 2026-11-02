<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="申报详情"/>
<c:set var="activeMenu" value="declarations"/>
<%@ include file="../common/head.jspf" %>
<c:set var="isOwner" value="${declaration.createdBy == currentUser.id}"/>

<div class="page-header">
    <div>
        <h1 class="page-title">申报详情</h1>
        <p class="page-subtitle mono"><c:out value="${declaration.declarationNo}"/></p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/declarations.action">返回列表</a>
        <c:if test="${isOwner && declaration.editable && canEdit}">
            <a class="btn btn-outline-secondary" href="${ctx}/declarations/edit.action?id=${declaration.id}">编辑草稿</a>
        </c:if>
        <c:if test="${isOwner && declaration.editable && canEdit}">
            <form class="inline-form" method="post" action="${ctx}/declarations/submit.action"
                  data-confirm="提交后将进入审核流程，期间不能编辑。确认提交？">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="id" value="${declaration.id}">
                <button class="btn btn-primary" type="submit">提交审核</button>
            </form>
        </c:if>
        <c:if test="${declaration.pending && (isOwner || currentUser.admin)}">
            <form class="inline-form" method="post" action="${ctx}/declarations/withdraw.action"
                  data-confirm="确认撤回该申报？撤回后可继续编辑并重新提交。">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="id" value="${declaration.id}">
                <button class="btn btn-outline-secondary" type="submit">撤回申报</button>
            </form>
        </c:if>
    </div>
</div>

<div class="detail-grid">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">基本信息</h2>
            <span class="status-badge ${declaration.statusBadgeClass}"><c:out value="${declaration.statusLabel}"/></span>
        </div>
        <dl class="detail-list">
            <div>
                <dt>纳税人</dt>
                <dd><c:out value="${declaration.taxpayerNameSnapshot}"/></dd>
            </div>
            <div>
                <dt>申报类型</dt>
                <dd><c:out value="${declaration.declarationTypeSnapshot}"/></dd>
            </div>
            <div>
                <dt>申报期间</dt>
                <dd class="mono">${declaration.periodStart} 至 ${declaration.periodEnd}</dd>
            </div>
            <div>
                <dt>模拟金额合计</dt>
                <dd><fmt:formatNumber value="${declaration.declaredAmount}" type="number" minFractionDigits="2" maxFractionDigits="2" groupingUsed="true"/> 元</dd>
            </div>
            <div>
                <dt>表单模板版本</dt>
                <dd>V${declaration.templateVersionNo}</dd>
            </div>
            <div>
                <dt>经办人</dt>
                <dd><c:out value="${declaration.createdByName}"/></dd>
            </div>
            <div>
                <dt>创建时间</dt>
                <dd><fmt:formatDate value="${declaration.createdAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
            <div>
                <dt>最近提交时间</dt>
                <dd><fmt:formatDate value="${declaration.submittedAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
        </dl>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">审核进度</h2>
        </div>
        <ol class="timeline">
            <li class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-body">
                    <div class="timeline-title">创建申报</div>
                    <div class="timeline-meta">
                        <c:out value="${declaration.createdByName}"/> ·
                        <fmt:formatDate value="${declaration.createdAt}" pattern="yyyy-MM-dd HH:mm"/>
                    </div>
                </div>
            </li>
            <c:forEach var="review" items="${declaration.reviews}">
                <li class="timeline-item">
                    <div class="timeline-dot ${review.reviewAction == 'APPROVE' ? 'dot-success' : 'dot-danger'}"></div>
                    <div class="timeline-body">
                        <div class="timeline-title"><c:out value="${review.actionLabel}"/></div>
                        <div class="timeline-meta">
                            <c:out value="${review.reviewerName}"/> ·
                            <fmt:formatDate value="${review.reviewedAt}" pattern="yyyy-MM-dd HH:mm"/>
                        </div>
                        <c:if test="${not empty review.reviewComment}">
                            <div class="timeline-comment"><c:out value="${review.reviewComment}"/></div>
                        </c:if>
                    </div>
                </li>
            </c:forEach>
            <c:if test="${declaration.status == 'DRAFT'}">
                <li class="timeline-item">
                    <div class="timeline-dot dot-muted"></div>
                    <div class="timeline-body">
                        <div class="timeline-title">尚未提交</div>
                        <div class="timeline-meta">填写完成后可保存草稿或提交审核</div>
                    </div>
                </li>
            </c:if>
            <c:if test="${declaration.status == 'WITHDRAWN'}">
                <li class="timeline-item">
                    <div class="timeline-dot dot-muted"></div>
                    <div class="timeline-body">
                        <div class="timeline-title">已撤回</div>
                        <div class="timeline-meta">经办人撤回了申报，可编辑后重新提交</div>
                    </div>
                </li>
            </c:if>
            <c:if test="${declaration.status == 'PENDING_REVIEW'}">
                <li class="timeline-item">
                    <div class="timeline-dot dot-warning"></div>
                    <div class="timeline-body">
                        <div class="timeline-title">等待审核</div>
                        <div class="timeline-meta">审核人员处理后在此显示结果</div>
                    </div>
                </li>
            </c:if>
        </ol>
    </section>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">申报表单内容（模板 V${declaration.templateVersionNo} 快照）</h2>
    </div>
    <div class="table-responsive">
        <table class="table app-table align-middle">
            <thead>
            <tr>
                <th style="width: 30%">字段</th>
                <th>填写内容</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="field" items="${declaration.templateFields}">
                <tr>
                    <td><c:out value="${field.fieldLabel}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${field.fieldType == 'BOOLEAN'}">
                                ${declaration.formValues[field.fieldCode] == 'true' ? '是' : '否'}
                            </c:when>
                            <c:when test="${empty declaration.formValues[field.fieldCode]}">
                                <span class="text-muted">未填写</span>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${declaration.formValues[field.fieldCode]}"/>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty declaration.templateFields}">
                <tr>
                    <td colspan="2" class="empty-cell">该申报对应的模板字段不存在或已不可用。</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</section>

<%@ include file="../common/footer.jspf" %>
