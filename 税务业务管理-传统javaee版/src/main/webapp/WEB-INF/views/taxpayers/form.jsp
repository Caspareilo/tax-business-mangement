<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${isNew ? '新增纳税人' : '编辑纳税人'}"/>
<c:set var="activeMenu" value="taxpayers"/>
<%@ include file="../common/head.jspf" %>

<div class="page-header">
    <div>
        <h1 class="page-title">${isNew ? '新增纳税人' : '编辑纳税人'}</h1>
        <p class="page-subtitle">全部字段仅用于课程演示，请勿录入真实单位或个人信息。</p>
    </div>
    <div class="page-actions">
        <a class="btn btn-outline-secondary" href="${ctx}/taxpayers.action">返回列表</a>
    </div>
</div>

<form method="post" action="${ctx}/taxpayers/save.action" novalidate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <c:if test="${not isNew}">
        <input type="hidden" name="id" value="${formTaxpayer.id}">
    </c:if>
    <section class="panel">
        <div class="panel-header">
            <h2 class="panel-title">纳税人信息</h2>
        </div>
        <div class="form-grid">
            <div class="form-field">
                <label class="form-label" for="taxpayerCode">纳税人编号 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.taxpayerCode ? 'is-invalid' : ''}"
                       id="taxpayerCode" name="taxpayerCode" type="text" maxlength="40"
                       value="<c:out value='${formTaxpayer.taxpayerCode}'/>"
                       ${isNew ? '' : 'readonly'}>
                <div class="field-hint">示例：DEMO-COMP-001。编号创建后不可修改。</div>
                <c:if test="${not empty fieldErrors.taxpayerCode}">
                    <div class="field-error"><c:out value="${fieldErrors.taxpayerCode}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="taxpayerName">纳税人名称 <span class="required-mark">*</span></label>
                <input class="form-control ${not empty fieldErrors.taxpayerName ? 'is-invalid' : ''}"
                       id="taxpayerName" name="taxpayerName" type="text" maxlength="200"
                       value="<c:out value='${formTaxpayer.taxpayerName}'/>">
                <c:if test="${not empty fieldErrors.taxpayerName}">
                    <div class="field-error"><c:out value="${fieldErrors.taxpayerName}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="taxpayerType">主体类型 <span class="required-mark">*</span></label>
                <select class="form-select ${not empty fieldErrors.taxpayerType ? 'is-invalid' : ''}"
                        id="taxpayerType" name="taxpayerType">
                    <option value="COMPANY" ${formTaxpayer.taxpayerType == 'COMPANY' ? 'selected' : ''}>企业</option>
                    <option value="INDIVIDUAL" ${formTaxpayer.taxpayerType == 'INDIVIDUAL' ? 'selected' : ''}>个体工商户</option>
                    <option value="INSTITUTION" ${formTaxpayer.taxpayerType == 'INSTITUTION' ? 'selected' : ''}>事业单位</option>
                </select>
                <c:if test="${not empty fieldErrors.taxpayerType}">
                    <div class="field-error"><c:out value="${fieldErrors.taxpayerType}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="registrationStatus">登记状态 <span class="required-mark">*</span></label>
                <select class="form-select ${not empty fieldErrors.registrationStatus ? 'is-invalid' : ''}"
                        id="registrationStatus" name="registrationStatus">
                    <option value="ACTIVE" ${formTaxpayer.registrationStatus == 'ACTIVE' ? 'selected' : ''}>正常</option>
                    <option value="DISABLED" ${formTaxpayer.registrationStatus == 'DISABLED' ? 'selected' : ''}>停用</option>
                </select>
                <c:if test="${not empty fieldErrors.registrationStatus}">
                    <div class="field-error"><c:out value="${fieldErrors.registrationStatus}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="contactName">联系人</label>
                <input class="form-control ${not empty fieldErrors.contactName ? 'is-invalid' : ''}"
                       id="contactName" name="contactName" type="text" maxlength="100"
                       value="<c:out value='${formTaxpayer.contactName}'/>">
                <c:if test="${not empty fieldErrors.contactName}">
                    <div class="field-error"><c:out value="${fieldErrors.contactName}"/></div>
                </c:if>
            </div>
            <div class="form-field">
                <label class="form-label" for="contactPhone">联系电话</label>
                <input class="form-control ${not empty fieldErrors.contactPhone ? 'is-invalid' : ''}"
                       id="contactPhone" name="contactPhone" type="text" maxlength="40"
                       value="<c:out value='${formTaxpayer.contactPhone}'/>">
                <div class="field-hint">演示数据请使用 000 开头的虚构号码。</div>
                <c:if test="${not empty fieldErrors.contactPhone}">
                    <div class="field-error"><c:out value="${fieldErrors.contactPhone}"/></div>
                </c:if>
            </div>
            <div class="form-field form-field-wide">
                <label class="form-label" for="remark">备注</label>
                <textarea class="form-control ${not empty fieldErrors.remark ? 'is-invalid' : ''}"
                          id="remark" name="remark" rows="3" maxlength="500"><c:out value="${formTaxpayer.remark}"/></textarea>
                <c:if test="${not empty fieldErrors.remark}">
                    <div class="field-error"><c:out value="${fieldErrors.remark}"/></div>
                </c:if>
            </div>
        </div>
    </section>
    <div class="form-actions">
        <button class="btn btn-primary" type="submit">保存</button>
        <a class="btn btn-outline-secondary" href="${ctx}/taxpayers.action">取消</a>
    </div>
</form>

<%@ include file="../common/footer.jspf" %>
