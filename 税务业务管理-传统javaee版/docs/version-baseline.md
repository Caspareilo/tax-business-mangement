# Java EE 8 技术基线与验证

## 固定版本

| 组件 | 固定值 | 配置位置 |
| --- | --- | --- |
| Java | JDK 11 | `pom.xml`、`Dockerfile` |
| Servlet | Servlet 4.0 / `javax.servlet` | `pom.xml`、`web.xml` |
| JSP | JSP 2.3 / `javax.servlet.jsp` | `pom.xml` |
| Tomcat | 9.0.123 | `Dockerfile`、`third_party/` |
| Maven | 3.8+，容器固定 3.9.9 | `Dockerfile` |
| MySQL | 8.0.46 | `compose.yaml` |
| Struts | 2.5.33 | `pom.xml` |
| 打包格式 | WAR | `pom.xml` |

## 验证步骤

```powershell
mvn clean test
mvn clean package
powershell -ExecutionPolicy Bypass -File .\scripts\verify-baseline.ps1
docker compose up -d --build
docker compose ps
```

成功标准：

1. 30 个自动化测试全部通过。
2. `target/tax-business-system.war` 构建成功。
3. 基线脚本输出 `Java EE 8 baseline check PASSED`。
4. Docker 中 MySQL 8.0.46 和 Tomcat 9 均为运行或健康状态。
5. 浏览器访问 `http://localhost:18082/hello?name=吴学孟` 返回 200。

## 隔离规则

- 不使用 Jakarta EE 9+ Java Web 命名空间。
- 不部署到 Tomcat 10+。
- 不把 Spring Boot 3、Vue 3 项目目录加入课程 Git 仓库。
- 不把 JDK 17/21、Tomcat 10+ 或 MySQL 9 写入课程部署配置。
