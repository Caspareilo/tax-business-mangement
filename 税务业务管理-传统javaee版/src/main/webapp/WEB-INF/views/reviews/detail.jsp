<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="审核详情"/>
<c:set var="activeMenu" value="reviews"/>
<%@ include file="../common/head.jspf" %>
<c:set var="isSelf" value="${declaration.createdBy == currentUser.id}"/>

<div class="page-header">
    <div>
        <h1 class="page-title">审核详情</h1>
        <p class="page-subtitle mono"><c:out value="${declaration.declarationNo}"/></p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/reviews/pending.action">返回待审核列表</a>
        <a class="btn btn-outline-secondary" href="${ctx}/declarations/detail.action?id=${declaration.id}">完整申报视图</a>
    </div>
</div>

<c:if test="${isSelf}">
    <div class="alert alert-warning" role="alert">该申报由您本人创建，按职责分离规则不能由您审核。</div>
</c:if>
<c:if test="${not declaration.pending}">
    <div class="alert alert-info" role="status">
        该申报当前状态为“<c:out value="${declaration.statusLabel}"/>”，不再处于待审核状态，不能再次审核。
    </div>
</c:if>

<div class="detail-grid">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">申报信息</h2>
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
                <dt>经办人</dt>
                <dd><c:out value="${declaration.createdByName}"/></dd>
            </div>
            <div>
                <dt>提交时间</dt>
                <dd><fmt:formatDate value="${declaration.submittedAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
            <div>
                <dt>模板版本</dt>
                <dd>V${declaration.templateVersionNo}</dd>
            </div>
        </dl>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">审核操作</h2>
        </div>
        <c:choose>
            <c:when test="${declaration.pending && not isSelf}">
                <form method="post" action="${ctx}/reviews/approve.action" class="mb-4"
                      data-confirm="确认审核通过该申报？">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${declaration.id}">
                    <div class="mb-2">
                        <label class="form-label" for="approveComment">审核意见（通过时可选）</label>
                        <textarea class="form-control" id="approveComment" name="comment" rows="3"
                                  maxlength="1000" placeholder="例如：资料齐全，金额核对无误。"></textarea>
                    </div>
                    <button class="btn btn-success" type="submit">审核通过</button>
                </form>
                <form method="post" action="${ctx}/reviews/return.action"
                      data-confirm="确认退回该申报？退回后经办人可修改并重新提交。">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="id" value="${declaration.id}">
                    <div class="mb-2">
                        <label class="form-label" for="returnComment">退回意见 <span class="required-mark">*</span></label>
                        <textarea class="form-control" id="returnComment" name="comment" rows="3"
                                  maxlength="1000" required placeholder="请说明需要补充或更正的内容"></textarea>
                    </div>
                    <button class="btn btn-outline-danger" type="submit">退回修改</button>
                </form>
            </c:when>
            <c:otherwise>
                <div class="empty-block">当前无法执行审核操作。</div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">表单内容</h2>
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
            </tbody>
        </table>
    </div>
</section>

<section class="panel">
    <div class="panel-header">
        <h2 class="panel-title">审核历史</h2>
    </div>
    <ol class="timeline">
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
        <c:if test="${empty declaration.reviews}">
            <li class="timeline-item">
                <div class="timeline-dot dot-muted"></div>
                <div class="timeline-body">
                    <div class="timeline-title">暂无审核记录</div>
                </div>
            </li>
        </c:if>
    </ol>
</section>

<%@ include file="../common/footer.jspf" %>
