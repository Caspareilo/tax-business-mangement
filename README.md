# 税务业务管理系统项目说明

> 作者：24软工2班 吴学孟 24110741085

## 项目定位

本项目是 Java EE 8 课程设计项目，代码位于 `税务业务管理-传统javaee版`。系统覆盖纳税人档案、申报类型、动态表单、申报提交、审核流转、统计分析、综合查询、CSV 导出和操作日志等功能。所有业务数据均为虚构演示数据。

课程仓库只发布传统 Java EE 8 项目，不包含 Spring Boot 3、Vue 3 或前后端分离实现。

## 统一技术基线

| 项目 | 固定版本或约束 |
| --- | --- |
| Java EE | Java EE 8 |
| Servlet API | `javax.servlet-api` 4.0.1 |
| JSP API | `javax.servlet.jsp-api` 2.3.3 |
| Java | JDK 11，编译目标 Java 11 |
| Web 容器 | Apache Tomcat 9.0.123 |
| Maven | 3.8+，Docker 构建使用 3.9.9 |
| 数据库 | MySQL 8.0.46 |
| Web 控制层 | Apache Struts 2.5.33 |
| 数据访问 | JDBC + PreparedStatement + 手写 DAO |
| 打包方式 | WAR |

项目只使用 `javax.*` Java Web API，不引入 Jakarta EE 9+，不部署到 Tomcat 10+。

## 核心功能

1. 登录、注册、退出、改密和基于角色的访问控制。
2. 纳税人档案新增、编辑、查询和启停。
3. 申报类型、动态表单模板及模板版本管理。
4. 申报草稿、提交、撤回、退回后修改和重新提交。
5. 审核通过、退回、审核意见、审核历史、自审拦截和并发校验。
6. 工作台、月度趋势、状态分布、类型金额和主体类型统计。
7. 组合查询、CSV 导出、公式注入防护和操作日志。

## 启动与验证

```powershell
docker compose up -d --build
```

默认访问地址：<http://localhost:18082>

本地验证：

```powershell
cd 税务业务管理-传统javaee版
mvn clean test
mvn clean package
powershell -ExecutionPolicy Bypass -File .\scripts\verify-baseline.ps1
```

当前自动化测试共 30 个，全部通过。Docker 已验证 JDK 11、Tomcat 9.0.123、MySQL 8.0.46、`/hello` HTTP 200 和五类演示账号登录。

## 文档

- [项目 README](税务业务管理-传统javaee版/README.md)
- [部署文档](税务业务管理-传统javaee版/DEPLOY.md)
- [技术基线与验证](税务业务管理-传统javaee版/docs/version-baseline.md)

## 配对部署验证

课程登记搭档：张兴利（24110741080）。搭档项目与本人项目的双向部署证据由双方在最终提交前分别确认，本文档不代替搭档签字。

---

作者：24软工2班 吴学孟 24110741085
