<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${isNew ? '新增申报类型' : '编辑申报类型'}"/>
<c:set var="activeMenu" value="types"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">${isNew ? '新增申报类型' : '编辑申报类型'}</h1>
        <p class="page-subtitle">类型编码创建后不可修改，停用/启用请使用列表中的状态按钮。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/declaration-types.action">返回列表</a>
    </div>
</div>

<form method="post" action="${ctx}/declaration-types/save.action" novalidate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <c:if test="${not isNew}">
        <input type="hidden" name="id" value="${formType.id}">
    </c:if>
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">类型信息</h2>
        </div>
        <div class="form-grid">
            <div class="form-field">
                <label class="form-label" for="typeCode">类型编码 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.typeCode ? 'is-invalid' : ''}"
                       id="typeCode" name="typeCode" type="text" maxlength="40"
                       value="<c:out value='${formType.typeCode}'/>" ${isNew ? '' : 'readonly'}>
                <div class="field-hint">大写字母、数字与下划线，示例：VAT_GENERAL。</div>
                <c:if test="${not empty fieldErrors.typeCode}">
                    <div class="field-error"><c:out value="${fieldErrors.typeCode}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="typeName">类型名称 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.typeName ? 'is-invalid' : ''}"
                       id="typeName" name="typeName" type="text" maxlength="100"
                       value="<c:out value='${formType.typeName}'/>">
                <c:if test="${not empty fieldErrors.typeName}">
                    <div class="field-error"><c:out value="${fieldErrors.typeName}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="deadlineDay">模拟申报期限（日） <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.deadlineDay ? 'is-invalid' : ''}"
                       id="deadlineDay" name="deadlineDay" type="number" min="1" max="28"
                       value="${formType.deadlineDay}">
                <div class="field-hint">1-28 之间的整数，仅用于演示提醒文案。</div>
                <c:if test="${not empty fieldErrors.deadlineDay}">
                    <div class="field-error"><c:out value="${fieldErrors.deadlineDay}"/></div>
                </c:if>
            </div>
            <div class="form-field form-field-wide">
                <label class="form-label" for="description">说明</label>
                <textarea class="form-control ${not empty fieldErrors.description ? 'is-invalid' : ''}"
                          id="description" name="description" rows="4"
                          maxlength="500"><c:out value="${formType.description}"/></textarea>
                <c:if test="${not empty fieldErrors.description}">
                    <div class="field-error"><c:out value="${fieldErrors.description}"/></div>
                </c:if>
            </div>
        </div>
    </section>
    <div class="form-actions">
        <button class="btn btn-primary" type="submit">保存</button>
        <a class="btn btn-outline-secondary" href="${ctx}/declaration-types.action">取消</a>
    </div>
</form>

<%@ include file="../common/footer.jspf" %>
