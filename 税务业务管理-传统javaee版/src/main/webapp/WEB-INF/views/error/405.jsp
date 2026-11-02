<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>405 请求方式不支持 - 税务业务管理系统</title>
    <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
    <link rel="stylesheet" href="${ctx}/assets/css/views.css">
</head>
<body class="error-page">
<main class="error-card">
    <div class="error-code">405</div>
    <h1>请求方式不支持</h1>
    <p><c:out value="${empty errorMessage ? '请返回系统后重新操作。' : errorMessage}"/></p>
    <div class="error-actions">
        <a class="btn btn-primary" href="${ctx}/dashboard.action">返回工作台</a>
    </div>
</main>
</body>
</html>
