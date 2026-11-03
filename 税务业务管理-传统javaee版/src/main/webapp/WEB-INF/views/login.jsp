<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>登录与注册 - 税务业务管理系统</title>
    <link rel="icon" href="${ctx}/assets/favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=20261103-register">
    <link rel="stylesheet" href="${ctx}/assets/css/views.css?v=20261103-register">
    <script src="${ctx}/assets/js/login.js?v=20261103-register" defer></script>
</head>
<body class="login-page" data-initial-login-tab="${empty activeLoginTab ? 'account' : activeLoginTab}">
<main class="login-shell">
    <section class="login-intro" aria-labelledby="loginBrandTitle">
        <div class="login-intro-inner">
            <h1 id="loginBrandTitle">税务业务管理<br>高效申报与审核</h1>
            <img class="login-illustration" src="${ctx}/assets/login-illustration.svg"
                 alt="税务申报、审核、统计和纳税人档案组成的业务管理插画">
            <ul class="login-feature-list" aria-label="系统能力">
                <li>申报</li>
                <li>审核</li>
                <li>统计</li>
                <li>审计</li>
            </ul>
            <p class="login-note">课程设计演示系统 · 数据均为虚构</p>
            <p class="login-copyright">Designed by 吴学孟 · 仅限课程演示使用</p>
        </div>
    </section>

    <section class="login-panel" aria-labelledby="loginHeading">
        <div class="login-card">
            <h2 class="login-heading" id="loginHeading">税务业务管理平台</h2>

            <div class="login-tabs" role="tablist" aria-label="账号操作">
                <button class="login-tab ${activeLoginTab == 'register' ? '' : 'active'}"
                        id="accountTab" type="button" role="tab"
                        aria-selected="${activeLoginTab == 'register' ? 'false' : 'true'}"
                        aria-controls="accountPanel" data-login-tab="account">
                    账号登录
                </button>
                <button class="login-tab ${activeLoginTab == 'register' ? 'active' : ''}"
                        id="registerTab" type="button" role="tab"
                        aria-selected="${activeLoginTab == 'register' ? 'true' : 'false'}"
                        aria-controls="registerPanel" data-login-tab="register">
                    注册账号
                </button>
                <button class="login-tab" id="roleTab" type="button" role="tab"
                        aria-selected="false" aria-controls="rolePanel" data-login-tab="role">
                    角色说明
                </button>
            </div>

            <div class="login-tab-panel" id="accountPanel" role="tabpanel" aria-labelledby="accountTab">
                <c:if test="${not empty infoMessage}">
                    <div class="alert alert-info" role="status"><c:out value="${infoMessage}"/></div>
                </c:if>
                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div>
                </c:if>

                <form id="loginForm" method="post" action="${ctx}/auth/login.action" novalidate>
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <div class="login-form-field">
                        <label class="form-label" for="username">用户名</label>
                        <div class="login-input-wrap">
                            <svg class="login-input-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <circle cx="12" cy="8" r="4" fill="none" stroke="currentColor" stroke-width="1.8"/>
                                <path d="M4.8 20c.7-4 3.3-6 7.2-6s6.5 2 7.2 6" fill="none"
                                      stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                            <input class="form-control" id="username" name="username" type="text"
                                   maxlength="50" autocomplete="username" required
                                   placeholder="请输入登录账号"
                                   value="<c:out value='${username}'/>" autofocus>
                        </div>
                    </div>
                    <div class="login-form-field">
                        <label class="form-label" for="password">密码</label>
                        <div class="login-input-wrap">
                            <svg class="login-input-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <rect x="5" y="10" width="14" height="10" rx="2" fill="none"
                                      stroke="currentColor" stroke-width="1.8"/>
                                <path d="M8 10V7a4 4 0 0 1 8 0v3" fill="none"
                                      stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                            <input class="form-control" id="password" name="password" type="password"
                                   maxlength="100" autocomplete="current-password" required
                                   placeholder="请输入密码">
                            <button class="password-toggle" type="button" data-password-toggle
                                    aria-label="显示密码" aria-pressed="false">
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="M2.8 12s3.2-5 9.2-5 9.2 5 9.2 5-3.2 5-9.2 5-9.2-5-9.2-5z"
                                          fill="none" stroke="currentColor" stroke-width="1.8"/>
                                    <circle cx="12" cy="12" r="2.5" fill="none"
                                            stroke="currentColor" stroke-width="1.8"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                    <button class="btn btn-primary w-100 login-submit" id="loginSubmit" type="submit">
                        登录
                    </button>
                </form>
                <section class="login-test-accounts" aria-labelledby="testAccountsTitle">
                    <div class="login-test-head">
                        <strong id="testAccountsTitle">测试账号 · 点击即登录</strong>
                        <span>仅演示环境</span>
                    </div>
                    <div class="test-account-grid">
                        <button class="test-account" type="button" data-quick-login
                                data-username="admin" data-password="Admin@123"
                                title="admin / Admin@123，点击自动登录" aria-label="以系统管理员 admin 登录">
                            <span class="test-account-role">管理员</span>
                            <span class="test-account-user">admin</span>
                        </button>
                        <button class="test-account" type="button" data-quick-login
                                data-username="clerk1" data-password="Clerk@123"
                                title="clerk1 / Clerk@123，点击自动登录" aria-label="以申报经办员 clerk1 登录">
                            <span class="test-account-role">经办人</span>
                            <span class="test-account-user">clerk1</span>
                        </button>
                        <button class="test-account" type="button" data-quick-login
                                data-username="reviewer1" data-password="Reviewer@123"
                                title="reviewer1 / Reviewer@123，点击自动登录" aria-label="以申报审核员 reviewer1 登录">
                            <span class="test-account-role">审核人</span>
                            <span class="test-account-user">reviewer1</span>
                        </button>
                        <button class="test-account" type="button" data-quick-login
                                data-username="analyst1" data-password="Analyst@123"
                                title="analyst1 / Analyst@123，点击自动登录" aria-label="以统计分析员 analyst1 登录">
                            <span class="test-account-role">分析员</span>
                            <span class="test-account-user">analyst1</span>
                        </button>
                        <button class="test-account" type="button" data-quick-login
                                data-username="viewer1" data-password="Viewer@123"
                                title="viewer1 / Viewer@123，点击自动登录" aria-label="以只读访客 viewer1 登录">
                            <span class="test-account-role">只读</span>
                            <span class="test-account-user">viewer1</span>
                        </button>
                    </div>
                </section>
            </div>

            <div class="login-tab-panel" id="registerPanel" role="tabpanel"
                 aria-labelledby="registerTab" hidden>
                <p class="login-subtitle">
                    新账号默认获得“只读访客”权限，注册后可联系管理员调整角色。
                </p>
                <c:if test="${not empty registrationError}">
                    <div class="alert alert-danger" role="alert">
                        <c:out value="${registrationError}"/>
                    </div>
                </c:if>

                <form id="registerForm" class="login-register-form" method="post"
                      action="${ctx}/auth/register.action" novalidate>
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <div class="login-form-field">
                        <label class="form-label" for="registerDisplayName">姓名或显示名称</label>
                        <div class="login-input-wrap">
                            <svg class="login-input-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <circle cx="12" cy="8" r="4" fill="none"
                                        stroke="currentColor" stroke-width="1.8"/>
                                <path d="M4.8 20c.7-4 3.3-6 7.2-6s6.5 2 7.2 6"
                                      fill="none" stroke="currentColor" stroke-width="1.8"
                                      stroke-linecap="round"/>
                            </svg>
                            <input class="form-control ${not empty registrationFieldErrors.displayName ? 'is-invalid' : ''}"
                                   id="registerDisplayName" name="displayName" type="text"
                                   maxlength="100" autocomplete="name" required
                                   placeholder="例如：李同学"
                                   value="<c:out value='${registerDisplayName}'/>">
                        </div>
                        <c:if test="${not empty registrationFieldErrors.displayName}">
                            <div class="invalid-feedback d-block">
                                <c:out value="${registrationFieldErrors.displayName}"/>
                            </div>
                        </c:if>
                    </div>
                    <div class="login-form-field">
                        <label class="form-label" for="registerUsername">登录账号</label>
                        <div class="login-input-wrap">
                            <svg class="login-input-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <circle cx="12" cy="8" r="4" fill="none"
                                        stroke="currentColor" stroke-width="1.8"/>
                                <path d="M4.8 20c.7-4 3.3-6 7.2-6s6.5 2 7.2 6"
                                      fill="none" stroke="currentColor" stroke-width="1.8"
                                      stroke-linecap="round"/>
                            </svg>
                            <input class="form-control ${not empty registrationFieldErrors.username ? 'is-invalid' : ''}"
                                   id="registerUsername" name="username" type="text"
                                   minlength="3" maxlength="64" autocomplete="username" required
                                   pattern="[A-Za-z0-9_.-]{3,64}"
                                   placeholder="3-64 位字母、数字或 _ . -"
                                   value="<c:out value='${registerUsername}'/>">
                        </div>
                        <c:if test="${not empty registrationFieldErrors.username}">
                            <div class="invalid-feedback d-block">
                                <c:out value="${registrationFieldErrors.username}"/>
                            </div>
                        </c:if>
                    </div>
                    <div class="login-form-field">
                        <label class="form-label" for="registerPassword">设置密码</label>
                        <div class="login-input-wrap">
                            <svg class="login-input-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <rect x="5" y="10" width="14" height="10" rx="2" fill="none"
                                      stroke="currentColor" stroke-width="1.8"/>
                                <path d="M8 10V7a4 4 0 0 1 8 0v3" fill="none"
                                      stroke="currentColor" stroke-width="1.8"
                                      stroke-linecap="round"/>
                            </svg>
                            <input class="form-control ${not empty registrationFieldErrors.password ? 'is-invalid' : ''}"
                                   id="registerPassword" name="password" type="password"
                                   minlength="8" maxlength="64" autocomplete="new-password" required
                                   placeholder="至少 8 位，包含字母和数字">
                            <button class="password-toggle" type="button" data-password-toggle
                                    data-password-target="registerPassword"
                                    aria-label="显示密码" aria-pressed="false">
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="M2.8 12s3.2-5 9.2-5 9.2 5 9.2 5-3.2 5-9.2 5-9.2-5-9.2-5z"
                                          fill="none" stroke="currentColor" stroke-width="1.8"/>
                                    <circle cx="12" cy="12" r="2.5" fill="none"
                                            stroke="currentColor" stroke-width="1.8"/>
                                </svg>
                            </button>
                        </div>
                        <c:if test="${not empty registrationFieldErrors.password}">
                            <div class="invalid-feedback d-block">
                                <c:out value="${registrationFieldErrors.password}"/>
                            </div>
                        </c:if>
                    </div>
                    <div class="login-form-field">
                        <label class="form-label" for="registerConfirmPassword">确认密码</label>
                        <div class="login-input-wrap">
                            <svg class="login-input-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <rect x="5" y="10" width="14" height="10" rx="2" fill="none"
                                      stroke="currentColor" stroke-width="1.8"/>
                                <path d="M8 10V7a4 4 0 0 1 8 0v3" fill="none"
                                      stroke="currentColor" stroke-width="1.8"
                                      stroke-linecap="round"/>
                            </svg>
                            <input class="form-control ${not empty registrationFieldErrors.confirmPassword ? 'is-invalid' : ''}"
                                   id="registerConfirmPassword" name="confirmPassword" type="password"
                                   minlength="8" maxlength="64" autocomplete="new-password" required
                                   placeholder="请再次输入密码">
                        </div>
                        <c:if test="${not empty registrationFieldErrors.confirmPassword}">
                            <div class="invalid-feedback d-block">
                                <c:out value="${registrationFieldErrors.confirmPassword}"/>
                            </div>
                        </c:if>
                    </div>
                    <button class="btn btn-primary w-100 login-submit login-register-submit"
                            type="submit">
                        创建只读账号
                    </button>
                </form>
            </div>

            <div class="login-tab-panel" id="rolePanel" role="tabpanel" aria-labelledby="roleTab" hidden>
                <p class="login-subtitle">回到“账号登录”后，点击测试账号即可进入对应角色。</p>
                <div class="role-guide">
                    <div class="role-guide-item">
                        <span class="role-guide-icon" aria-hidden="true">管</span>
                        <span>
                            <span class="role-guide-name">系统管理员 · admin</span>
                            <span class="role-guide-desc">用户、角色、字典、操作审计与基础数据管理</span>
                        </span>
                    </div>
                    <div class="role-guide-item">
                        <span class="role-guide-icon" aria-hidden="true">办</span>
                        <span>
                            <span class="role-guide-name">申报经办员 · clerk1</span>
                            <span class="role-guide-desc">新建、保存、提交和撤回本人申报</span>
                        </span>
                    </div>
                    <div class="role-guide-item">
                        <span class="role-guide-icon" aria-hidden="true">审</span>
                        <span>
                            <span class="role-guide-name">申报审核员 · reviewer1</span>
                            <span class="role-guide-desc">处理待审核申报，通过或填写意见后退回</span>
                        </span>
                    </div>
                    <div class="role-guide-item">
                        <span class="role-guide-icon" aria-hidden="true">析</span>
                        <span>
                            <span class="role-guide-name">统计分析员 · analyst1</span>
                            <span class="role-guide-desc">查看统计报表、综合查询并导出申报数据</span>
                        </span>
                    </div>
                    <div class="role-guide-item">
                        <span class="role-guide-icon" aria-hidden="true">读</span>
                        <span>
                            <span class="role-guide-name">只读访客 · viewer1</span>
                            <span class="role-guide-desc">仅查看工作台和申报记录，不能新建或修改数据</span>
                        </span>
                    </div>
                </div>
            </div>

            <p class="login-footnote">
                本入口仅用于课程教学与演示，请勿录入真实纳税人或个人信息
            </p>
        </div>
    </section>
</main>
</body>
</html>
