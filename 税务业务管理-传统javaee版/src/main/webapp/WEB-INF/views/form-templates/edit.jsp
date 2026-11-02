<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${isNew ? '新建表单模板' : '编辑模板草稿'}"/>
<c:set var="activeMenu" value="templates"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">${isNew ? '新建表单模板' : '编辑模板草稿'}</h1>
        <p class="page-subtitle">字段编码在表单提交后不可变更；发布后的模板不能原位修改，请使用“新建版本”。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/form-templates.action">返回列表</a>
        <c:if test="${not isNew}">
            <a class="btn btn-outline-secondary" href="${ctx}/form-templates/preview.action?id=${template.id}">预览</a>
        </c:if>
        <c:if test="${not isNew && template.draft}">
            <form class="inline-form" method="post" action="${ctx}/form-templates/publish.action"
                  data-confirm="发布后模板不可再修改。确认发布当前草稿？">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="id" value="${template.id}">
                <button class="btn btn-success" type="submit">发布模板</button>
            </form>
        </c:if>
    </div>
</div>

<c:if test="${not empty fieldErrors}">
    <div class="alert alert-danger" role="alert">
        <div><c:out value="${errorMessage}"/></div>
        <ul class="mb-0 mt-1">
            <c:forEach var="entry" items="${fieldErrors}">
                <li><c:out value="${entry.value}"/></li>
            </c:forEach>
        </ul>
    </div>
</c:if>

<form method="post" action="${ctx}/form-templates/save.action" novalidate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <c:if test="${not isNew}">
        <input type="hidden" name="id" value="${template.id}">
    </c:if>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">模板信息</h2>
        </div>
        <div class="form-grid">
            <c:choose>
                <c:when test="${isNew}">
                    <div class="form-field">
                        <label class="form-label" for="typeId">申报类型 <span class="required-mark">*</span></label>
                        <select class="form-select" id="typeId" name="typeId">
                            <option value="">请选择申报类型</option>
                            <c:forEach var="type" items="${types}">
                                <option value="${type.id}" ${template.declarationTypeId == type.id ? 'selected' : ''}>
                                    <c:out value="${type.typeName}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-field">
                        <label class="form-label" for="templateCode">模板编码 <span class="required-mark">*</span></label>
                        <input class="form-control" id="templateCode" name="templateCode" type="text"
                               maxlength="40" value="<c:out value='${template.templateCode}'/>">
                        <div class="field-hint">新建模板从 V1 开始，示例：VAT_GENERAL_FORM。</div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="form-field">
                        <label class="form-label">模板编码</label>
                        <input class="form-control" type="text" readonly
                               value="<c:out value='${template.templateCode}'/>">
                    </div>
                    <div class="form-field">
                        <label class="form-label">申报类型 / 版本</label>
                        <input class="form-control" type="text" readonly
                               value="<c:out value='${template.declarationTypeName}'/> / V${template.versionNo}">
                    </div>
                </c:otherwise>
            </c:choose>
            <div class="form-field">
                <label class="form-label" for="templateName">模板名称 <span class="required-mark">*</span></label>
                <input class="form-control" id="templateName" name="templateName" type="text"
                       maxlength="150" value="<c:out value='${template.templateName}'/>">
            </div>
        </div>
    </section>

    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">字段配置</h2>
            <button class="btn btn-sm btn-outline-secondary" type="button" id="addFieldBtn">添加字段</button>
        </div>
        <div id="fieldRows">
            <c:choose>
                <c:when test="${empty template.fields}">
                    <div class="field-row">
                        <div class="field-row-number">1</div>
                        <div class="field-row-grid">
                            <div>
                                <label class="form-label">字段编码 <span class="required-mark">*</span></label>
                                <input class="form-control" name="fieldCode" type="text" maxlength="64"
                                       placeholder="例如 salesAmount">
                            </div>
                            <div>
                                <label class="form-label">字段名称 <span class="required-mark">*</span></label>
                                <input class="form-control" name="fieldLabel" type="text" maxlength="150"
                                       placeholder="例如 销售额">
                            </div>
                            <div>
                                <label class="form-label">字段类型</label>
                                <select class="form-select js-field-type" name="fieldType">
                                    <option value="TEXT">文本</option>
                                    <option value="INTEGER">整数</option>
                                    <option value="AMOUNT">金额</option>
                                    <option value="DATE">日期</option>
                                    <option value="RADIO">单选</option>
                                    <option value="MULTI">多选</option>
                                    <option value="BOOLEAN">是否</option>
                                </select>
                            </div>
                            <div class="field-row-check">
                                <label class="form-check">
                                    <input class="form-check-input" type="checkbox" name="fieldRequired" value="0">
                                    <span class="form-check-label">必填</span>
                                </label>
                            </div>
                        </div>
                        <div class="field-row-extra">
                            <div class="js-text-fields">
                                <label class="form-label">最大长度</label>
                                <input class="form-control" name="maxLength" type="number" min="1" max="2000"
                                       placeholder="默认 2000">
                            </div>
                            <div class="js-numeric-fields" hidden>
                                <label class="form-label">最小值</label>
                                <input class="form-control" name="minValue" type="text" maxlength="20">
                            </div>
                            <div class="js-numeric-fields" hidden>
                                <label class="form-label">最大值</label>
                                <input class="form-control" name="maxValue" type="text" maxlength="20">
                            </div>
                            <div class="js-options-fields" hidden>
                                <label class="form-label">选项（逗号分隔，至少 2 项）</label>
                                <input class="form-control" name="options" type="text" maxlength="1000"
                                       placeholder="例如 是,否">
                            </div>
                            <div>
                                <label class="form-label">帮助提示</label>
                                <input class="form-control" name="helpText" type="text" maxlength="200">
                            </div>
                            <div class="field-row-actions">
                                <button class="btn btn-sm btn-outline-secondary" type="button" data-remove-field>删除</button>
                            </div>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <c:forEach var="field" items="${template.fields}" varStatus="status">
                        <div class="field-row">
                            <div class="field-row-number">${status.index + 1}</div>
                            <div class="field-row-grid">
                                <div>
                                    <label class="form-label">字段编码 <span class="required-mark">*</span></label>
                                    <input class="form-control" name="fieldCode" type="text" maxlength="64"
                                           value="<c:out value='${field.fieldCode}'/>">
                                </div>
                                <div>
                                    <label class="form-label">字段名称 <span class="required-mark">*</span></label>
                                    <input class="form-control" name="fieldLabel" type="text" maxlength="150"
                                           value="<c:out value='${field.fieldLabel}'/>">
                                </div>
                                <div>
                                    <label class="form-label">字段类型</label>
                                    <select class="form-select js-field-type" name="fieldType">
                                        <option value="TEXT" ${field.fieldType == 'TEXT' ? 'selected' : ''}>文本</option>
                                        <option value="INTEGER" ${field.fieldType == 'INTEGER' ? 'selected' : ''}>整数</option>
                                        <option value="AMOUNT" ${field.fieldType == 'AMOUNT' ? 'selected' : ''}>金额</option>
                                        <option value="DATE" ${field.fieldType == 'DATE' ? 'selected' : ''}>日期</option>
                                        <option value="RADIO" ${field.fieldType == 'RADIO' ? 'selected' : ''}>单选</option>
                                        <option value="MULTI" ${field.fieldType == 'MULTI' ? 'selected' : ''}>多选</option>
                                        <option value="BOOLEAN" ${field.fieldType == 'BOOLEAN' ? 'selected' : ''}>是否</option>
                                    </select>
                                </div>
                                <div class="field-row-check">
                                    <label class="form-check">
                                        <input class="form-check-input" type="checkbox" name="fieldRequired"
                                               value="${status.index}" ${field.required ? 'checked' : ''}>
                                        <span class="form-check-label">必填</span>
                                    </label>
                                </div>
                            </div>
                            <div class="field-row-extra">
                                <div class="js-text-fields" ${field.fieldType == 'TEXT' ? '' : 'hidden'}>
                                    <label class="form-label">最大长度</label>
                                    <input class="form-control" name="maxLength" type="number" min="1" max="2000"
                                           value="${field.maxLength}">
                                </div>
                                <div class="js-numeric-fields"
                                     ${field.fieldType == 'INTEGER' || field.fieldType == 'AMOUNT' ? '' : 'hidden'}>
                                    <label class="form-label">最小值</label>
                                    <input class="form-control" name="minValue" type="text" maxlength="20"
                                           value="${field.minValue}">
                                </div>
                                <div class="js-numeric-fields"
                                     ${field.fieldType == 'INTEGER' || field.fieldType == 'AMOUNT' ? '' : 'hidden'}>
                                    <label class="form-label">最大值</label>
                                    <input class="form-control" name="maxValue" type="text" maxlength="20"
                                           value="${field.maxValue}">
                                </div>
                                <div class="js-options-fields"
                                     ${field.fieldType == 'RADIO' || field.fieldType == 'MULTI' ? '' : 'hidden'}>
                                    <label class="form-label">选项（逗号分隔，至少 2 项）</label>
                                    <input class="form-control" name="options" type="text" maxlength="1000"
                                           value="<c:out value='${field.optionsText}'/>">
                                </div>
                                <div>
                                    <label class="form-label">帮助提示</label>
                                    <input class="form-control" name="helpText" type="text" maxlength="200"
                                           value="<c:out value='${field.helpText}'/>">
                                </div>
                                <div class="field-row-actions">
                                    <button class="btn btn-sm btn-outline-secondary" type="button" data-remove-field>删除</button>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>
        <p class="field-hint mt-2">字段编码必须以字母开头，仅含字母、数字和下划线；单选/多选至少配置两个选项。</p>
    </section>

    <div class="form-actions">
        <button class="btn btn-primary" type="submit">保存草稿</button>
        <a class="btn btn-outline-secondary" href="${ctx}/form-templates.action">取消</a>
    </div>
</form>

<template id="fieldRowTemplate">
    <div class="field-row">
        <div class="field-row-number"></div>
        <div class="field-row-grid">
            <div>
                <label class="form-label">字段编码 <span class="required-mark">*</span></label>
                <input class="form-control" name="fieldCode" type="text" maxlength="64" placeholder="例如 salesAmount">
            </div>
            <div>
                <label class="form-label">字段名称 <span class="required-mark">*</span></label>
                <input class="form-control" name="fieldLabel" type="text" maxlength="150" placeholder="例如 销售额">
            </div>
            <div>
                <label class="form-label">字段类型</label>
                <select class="form-select js-field-type" name="fieldType">
                    <option value="TEXT">文本</option>
                    <option value="INTEGER">整数</option>
                    <option value="AMOUNT">金额</option>
                    <option value="DATE">日期</option>
                    <option value="RADIO">单选</option>
                    <option value="MULTI">多选</option>
                    <option value="BOOLEAN">是否</option>
                </select>
            </div>
            <div class="field-row-check">
                <label class="form-check">
                    <input class="form-check-input" type="checkbox" name="fieldRequired" value="0">
                    <span class="form-check-label">必填</span>
                </label>
            </div>
        </div>
        <div class="field-row-extra">
            <div class="js-text-fields">
                <label class="form-label">最大长度</label>
                <input class="form-control" name="maxLength" type="number" min="1" max="2000" placeholder="默认 2000">
            </div>
            <div class="js-numeric-fields" hidden>
                <label class="form-label">最小值</label>
                <input class="form-control" name="minValue" type="text" maxlength="20">
            </div>
            <div class="js-numeric-fields" hidden>
                <label class="form-label">最大值</label>
                <input class="form-control" name="maxValue" type="text" maxlength="20">
            </div>
            <div class="js-options-fields" hidden>
                <label class="form-label">选项（逗号分隔，至少 2 项）</label>
                <input class="form-control" name="options" type="text" maxlength="1000" placeholder="例如 是,否">
            </div>
            <div>
                <label class="form-label">帮助提示</label>
                <input class="form-control" name="helpText" type="text" maxlength="200">
            </div>
            <div class="field-row-actions">
                <button class="btn btn-sm btn-outline-secondary" type="button" data-remove-field>删除</button>
            </div>
        </div>
    </div>
</template>

<%@ include file="../common/footer.jspf" %>
