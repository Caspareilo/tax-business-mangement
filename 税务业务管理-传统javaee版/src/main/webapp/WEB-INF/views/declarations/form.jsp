<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="isEdit" value="${not empty form.id}"/>
<c:set var="pageTitle" value="${isEdit ? '编辑申报' : '新建申报'}"/>
<c:set var="activeMenu" value="declaration-create"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">${isEdit ? '编辑申报' : '新建申报'}</h1>
        <p class="page-subtitle">
            选择纳税人、申报类型与期间，按当前已发布表单模板填写内容。带 <span class="required-mark">*</span> 的为必填项。
        </p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/declarations.action">返回列表</a>
    </div>
</div>

<c:if test="${not empty template}">
    <div class="info-strip">
        使用表单模板：<strong><c:out value="${template.templateName}"/></strong>
        （版本 V${template.versionNo}，表单内容按该版本快照保存）
    </div>
</c:if>

<form method="post" action="${ctx}/declarations/save.action" novalidate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <c:if test="${isEdit}">
        <input type="hidden" name="id" value="${form.id}">
    </c:if>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">基本信息</h2>
        </div>
        <div class="form-grid">
            <div class="form-field">
                <label class="form-label" for="taxpayerId">纳税人 <span class="required-mark">*</span></label>
                <select class="form-select ${not empty fieldErrors.taxpayerId ? 'is-invalid' : ''}"
                        id="taxpayerId" name="taxpayerId">
                    <option value="">请选择纳税人</option>
                    <c:forEach var="taxpayer" items="${taxpayers}">
                        <option value="${taxpayer.id}" ${form.taxpayerId == taxpayer.id ? 'selected' : ''}>
                            <c:out value="${taxpayer.taxpayerName}"/>（<c:out value="${taxpayer.taxpayerCode}"/>）
                        </option>
                    </c:forEach>
                </select>
                <c:if test="${not empty fieldErrors.taxpayerId}">
                    <div class="field-error"><c:out value="${fieldErrors.taxpayerId}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="declarationTypeId">申报类型 <span class="required-mark">*</span></label>
                <c:choose>
                    <c:when test="${isEdit}">
                        <input type="hidden" name="declarationTypeId" value="${form.declarationTypeId}">
                        <input class="form-control" id="declarationTypeId" type="text" disabled
                               value="<c:out value='${declaration.declarationTypeSnapshot}'/>">
                        <div class="field-hint">申报类型在保存后不可修改。</div>
                    </c:when>
                    <c:otherwise>
                        <select class="form-select ${not empty fieldErrors.declarationTypeId ? 'is-invalid' : ''}"
                                id="declarationTypeId" name="declarationTypeId"
                                data-reload-on-change="${ctx}/declarations/create.action">
                            <option value="">请选择申报类型</option>
                            <c:forEach var="type" items="${types}">
                                <option value="${type.id}" ${form.declarationTypeId == type.id ? 'selected' : ''}>
                                    <c:out value="${type.typeName}"/>
                                </option>
                            </c:forEach>
                        </select>
                        <div class="field-hint">切换类型后页面会自动加载该类型已发布的表单模板。</div>
                        <noscript><div class="field-hint">未启用脚本时，请在选择类型后点击“重新加载表单”。</div></noscript>
                    </c:otherwise>
                </c:choose>
                <c:if test="${not empty fieldErrors.declarationTypeId}">
                    <div class="field-error"><c:out value="${fieldErrors.declarationTypeId}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="periodStart">期间开始 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.periodStart ? 'is-invalid' : ''}"
                       id="periodStart" name="periodStart" type="date" value="${form.periodStart}">
                <c:if test="${not empty fieldErrors.periodStart}">
                    <div class="field-error"><c:out value="${fieldErrors.periodStart}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="periodEnd">期间结束 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.periodEnd ? 'is-invalid' : ''}"
                       id="periodEnd" name="periodEnd" type="date" value="${form.periodEnd}">
                <c:if test="${not empty fieldErrors.periodEnd}">
                    <div class="field-error"><c:out value="${fieldErrors.periodEnd}"/></div>
                </c:if>
            </div>
        </div>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">申报表单内容</h2>
        </div>
        <c:choose>
            <c:when test="${empty template}">
                <div class="empty-block">
                    请先选择申报类型。系统会自动加载该类型已发布的表单模板；若类型尚未发布模板，请联系管理员。
                </div>
            </c:when>
            <c:when test="${empty template.fields}">
                <div class="empty-block">该模板暂未配置字段，无法填写申报内容。</div>
            </c:when>
            <c:otherwise>
                <div class="form-grid">
                    <c:forEach var="field" items="${template.fields}">
                        <c:if test="${field.enabled}">
                            <div class="form-field ${field.fieldType == 'TEXT' ? 'form-field-wide' : ''}">
                                <label class="form-label" for="field_${field.fieldCode}">
                                    <c:out value="${field.fieldLabel}"/>
                                    <c:if test="${field.required}"><span class="required-mark">*</span></c:if>
                                </label>
                                <c:set var="value" value="${form.formValues[field.fieldCode]}"/>
                                <c:choose>
                                    <c:when test="${field.fieldType == 'TEXT'}">
                                        <textarea class="form-control ${not empty fieldErrors[field.fieldCode] ? 'is-invalid' : ''}"
                                                  id="field_${field.fieldCode}" name="field_${field.fieldCode}"
                                                  rows="3" maxlength="${empty field.maxLength ? 2000 : field.maxLength}"
                                                  placeholder="<c:out value='${field.helpText}'/>"><c:out value="${value}"/></textarea>
                                    </c:when>
                                    <c:when test="${field.fieldType == 'INTEGER'}">
                                        <input class="form-control ${not empty fieldErrors[field.fieldCode] ? 'is-invalid' : ''}"
                                               id="field_${field.fieldCode}" name="field_${field.fieldCode}"
                                               type="number" step="1" value="<c:out value='${value}'/>">
                                    </c:when>
                                    <c:when test="${field.fieldType == 'AMOUNT'}">
                                        <input class="form-control ${not empty fieldErrors[field.fieldCode] ? 'is-invalid' : ''}"
                                               id="field_${field.fieldCode}" name="field_${field.fieldCode}"
                                               type="number" step="0.01" min="0" value="<c:out value='${value}'/>">
                                    </c:when>
                                    <c:when test="${field.fieldType == 'DATE'}">
                                        <input class="form-control ${not empty fieldErrors[field.fieldCode] ? 'is-invalid' : ''}"
                                               id="field_${field.fieldCode}" name="field_${field.fieldCode}"
                                               type="date" value="<c:out value='${value}'/>">
                                    </c:when>
                                    <c:when test="${field.fieldType == 'RADIO'}">
                                        <div class="option-group">
                                            <c:forEach var="option" items="${field.optionList}">
                                                <label class="form-check form-check-inline">
                                                    <input class="form-check-input" type="radio"
                                                           name="field_${field.fieldCode}"
                                                           value="<c:out value='${option}'/>"
                                                           ${value == option ? 'checked' : ''}>
                                                    <span class="form-check-label"><c:out value="${option}"/></span>
                                                </label>
                                            </c:forEach>
                                        </div>
                                    </c:when>
                                    <c:when test="${field.fieldType == 'MULTI'}">
                                        <c:set var="selectedRaw" value=",${value},"/>
                                        <div class="option-group">
                                            <c:forEach var="option" items="${field.optionList}">
                                                <c:set var="optionToken" value=",${option},"/>
                                                <label class="form-check form-check-inline">
                                                    <input class="form-check-input" type="checkbox"
                                                           name="field_${field.fieldCode}"
                                                           value="<c:out value='${option}'/>"
                                                           ${fn:contains(selectedRaw, optionToken) ? 'checked' : ''}>
                                                    <span class="form-check-label"><c:out value="${option}"/></span>
                                                </label>
                                            </c:forEach>
                                        </div>
                                    </c:when>
                                    <c:when test="${field.fieldType == 'BOOLEAN'}">
                                        <label class="form-check">
                                            <input class="form-check-input" type="checkbox"
                                                   id="field_${field.fieldCode}"
                                                   name="field_${field.fieldCode}" value="true"
                                                   ${value == 'true' ? 'checked' : ''}>
                                            <span class="form-check-label">是</span>
                                        </label>
                                    </c:when>
                                    <c:otherwise>
                                        <input class="form-control" id="field_${field.fieldCode}"
                                               name="field_${field.fieldCode}" type="text"
                                               value="<c:out value='${value}'/>">
                                    </c:otherwise>
                                </c:choose>
                                <c:if test="${not empty field.helpText}">
                                    <div class="field-hint"><c:out value="${field.helpText}"/></div>
                                </c:if>
                                <c:if test="${not empty field.minValue || not empty field.maxValue}">
                                    <div class="field-hint">
                                        允许范围：
                                        <c:out value="${empty field.minValue ? '不限' : field.minValue}"/>
                                        ~
                                        <c:out value="${empty field.maxValue ? '不限' : field.maxValue}"/>
                                    </div>
                                </c:if>
                                <c:if test="${not empty fieldErrors[field.fieldCode]}">
                                    <div class="field-error"><c:out value="${fieldErrors[field.fieldCode]}"/></div>
                                </c:if>
                            </div>
                        </c:if>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>

    <div class="form-actions">
        <c:if test="${not empty template}">
            <button class="btn btn-primary" type="submit" name="action" value="draft">保存草稿</button>
            <button class="btn btn-success" type="submit" name="action" value="submit"
                    data-confirm-button="提交后将进入审核流程。确认提交？">保存并提交审核</button>
        </c:if>
        <c:choose>
            <c:when test="${isEdit}">
                <a class="btn btn-outline-secondary" href="${ctx}/declarations/detail.action?id=${form.id}">取消</a>
            </c:when>
            <c:otherwise>
                <a class="btn btn-outline-secondary" href="${ctx}/declarations.action">取消</a>
            </c:otherwise>
        </c:choose>
    </div>
</form>

<%@ include file="../common/footer.jspf" %>
