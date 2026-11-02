<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>500 系统异常 - 税务业务管理系统</title>
    <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
    <link rel="stylesheet" href="${ctx}/assets/css/views.css">
</head>
<body class="error-page">
<main class="error-card">
    <div class="error-code">500</div>
    <h1>系统暂时无法处理该请求</h1>
    <p><c:out value="${empty errorMessage ? '系统出现异常，请稍后重试；如持续出现，请联系管理员并提供操作时间。' : errorMessage}"/></p>
    <div class="error-actions">
        <a class="btn btn-primary" href="${ctx}/dashboard.action">返回工作台</a>
        <a class="btn btn-outline-secondary" href="${ctx}/auth/login.action">重新登录</a>
    </div>
</main>
</body>
</html>
