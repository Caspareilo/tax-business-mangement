<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="模板预览"/>
<c:set var="activeMenu" value="templates"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title"><c:out value="${template.templateName}"/></h1>
        <p class="page-subtitle">
            <span class="mono"><c:out value="${template.templateCode}"/></span>
            · V${template.versionNo}
            · <c:out value="${template.declarationTypeName}"/>
            · <span class="status-badge ${template.published ? 'status-success' : (template.draft ? 'status-warning' : 'status-muted')}"><c:out value="${template.statusLabel}"/></span>
        </p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/form-templates.action">返回列表</a>
        <a class="btn btn-outline-secondary"
           href="${ctx}/form-templates/versions.action?code=${template.templateCode}">版本历史</a>
        <c:if test="${template.draft}">
            <a class="btn btn-outline-secondary" href="${ctx}/form-templates/edit.action?id=${template.id}">编辑草稿</a>
            <form class="inline-form" method="post" action="${ctx}/form-templates/publish.action"
                  data-confirm="发布后该模板不可再修改，并会停用同类型的旧发布版本。确认发布？">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="id" value="${template.id}">
                <button class="btn btn-primary" type="submit">发布模板</button>
            </form>
        </c:if>
        <c:if test="${not template.draft}">
            <form class="inline-form" method="post" action="${ctx}/form-templates/new-version.action"
                  data-confirm="将基于该版本创建新的草稿版本。继续？">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="id" value="${template.id}">
                <button class="btn btn-primary" type="submit">新建版本</button>
            </form>
        </c:if>
    </div>
</div>

<div class="detail-grid">
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">模板元数据</h2>
        </div>
        <dl class="detail-list">
            <div>
                <dt>创建人</dt>
                <dd><c:out value="${template.createdByName}"/></dd>
            </div>
            <div>
                <dt>创建时间</dt>
                <dd><fmt:formatDate value="${template.createdAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
            <div>
                <dt>发布时间</dt>
                <dd><fmt:formatDate value="${template.publishedAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
            </div>
            <div>
                <dt>字段数量</dt>
                <dd>${template.fields.size()}</dd>
            </div>
        </dl>
    </section>
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">填写效果预览</h2>
        </div>
        <div class="preview-form">
            <c:forEach var="field" items="${template.fields}">
                <div class="form-field">
                    <label class="form-label">
                        <c:out value="${field.fieldLabel}"/>
                        <c:if test="${field.required}"><span class="required-mark">*</span></c:if>
                        <span class="field-type-tag"><c:out value="${field.typeLabel}"/></span>
                    </label>
                    <c:choose>
                        <c:when test="${field.fieldType == 'TEXT'}">
                            <input class="form-control" type="text" disabled placeholder="文本输入">
                        </c:when>
                        <c:when test="${field.fieldType == 'INTEGER'}">
                            <input class="form-control" type="number" step="1" disabled placeholder="整数">
                        </c:when>
                        <c:when test="${field.fieldType == 'AMOUNT'}">
                            <input class="form-control" type="number" step="0.01" disabled placeholder="0.00">
                        </c:when>
                        <c:when test="${field.fieldType == 'DATE'}">
                            <input class="form-control" type="date" disabled>
                        </c:when>
                        <c:when test="${field.fieldType == 'BOOLEAN'}">
                            <label class="form-check">
                                <input class="form-check-input" type="checkbox" disabled>
                                <span class="form-check-label">是</span>
                            </label>
                        </c:when>
                        <c:otherwise>
                            <div class="option-group">
                                <c:forEach var="option" items="${field.optionList}">
                                    <label class="form-check form-check-inline">
                                        <input class="form-check-input" type="${field.fieldType == 'MULTI' ? 'checkbox' : 'radio'}"
                                               disabled>
                                        <span class="form-check-label"><c:out value="${option}"/></span>
                                    </label>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${not empty field.helpText}">
                        <div class="field-hint"><c:out value="${field.helpText}"/></div>
                    </c:if>
                    <c:if test="${not empty field.minValue || not empty field.maxValue}">
                        <div class="field-hint">
                            范围：<c:out value="${empty field.minValue ? '不限' : field.minValue}"/> ~
                            <c:out value="${empty field.maxValue ? '不限' : field.maxValue}"/>
                        </div>
                    </c:if>
                    <c:if test="${not empty field.maxLength}">
                        <div class="field-hint">最多 ${field.maxLength} 个字符</div>
                    </c:if>
                </div>
            </c:forEach>
        </div>
    </section>
</div>

<%@ include file="../common/footer.jspf" %>
