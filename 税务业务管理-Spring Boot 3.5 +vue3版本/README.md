> **部署流程见：[部署文档.md](部署文档.md)**

# 税务业务管理系统（教学模拟）

基于 Spring Boot 3.5 + Vue 3 的前后端分离课程项目，覆盖纳税人档案、申报表单、申报流转、审核处理、统计分析与操作审计的完整业务闭环。

> 当前 Vue 3 前端已与传统 Java EE 版统一为同一套企业后台视觉体系，但两个项目仍分别使用各自的 Vue / JSP 技术栈，不共享运行时框架。

> **教学声明**：本系统仅用于课程教学与演示，所有纳税人、申报、审核与金额数据均为虚构，不连接任何真实税务机关，不提交真实申报，不代表真实税法口径。

## 功能概览

| 模块 | 功能 |
| ---- | ---- |
| 工作台 | 申报总量、待审核、通过率、金额合计；趋势/状态/类型图表；待办与最近日志；数据来自后端实时聚合 |
| 用户与权限 | 登录/自助注册/登出/修改密码；注册账号默认只读访客；用户 CRUD、停用、重置密码；角色与权限分配；后端 `@PreAuthorize` 强制鉴权 |
| 纳税人 | 虚构纳税人档案 CRUD、启停、关联申报历史；编号唯一；有申报历史不可删除 |
| 申报类型 | 新增、编辑、启停、查询；停用类型不可用于新申报 |
| 表单模板 | 字段配置（文本/整数/金额/日期/单选/多选/布尔）、预览、发布、停用、版本历史 |
| 申报管理 | 草稿、编辑、提交、撤回、退回后修改；服务端校验必填/类型/范围/重复申报 |
| 审核管理 | 待办、详情、通过、退回、审核意见与历史；禁止自审；重复审核 409 |
| 统计分析 | 概览、趋势、状态、类型、主体类型；服务端聚合，无硬编码 |
| 综合查询 | 组合筛选、分页、白名单排序、详情；CSV 导出（BOM、防公式注入、5000 行上限） |
| 操作日志 | 登录、申报、审核、导出、改密等关键操作；不记录密码/Token；只读 |

## 技术栈

| 端 | 技术 |
| -- | ---- |
| 后端 | Java 17、Spring Boot 3.5.16、Spring Security 6、Jakarta Validation、MyBatis-Plus 3.5.18、MySQL、Flyway、JUnit 5 / MockMvc、springdoc-openapi 2.8.17 |
| 前端 | Vue 3.5、TypeScript 5.7、Vite 6、Element Plus 2.9、Pinia 3、Vue Router 4、Axios、ECharts 5 |
| 工程 | ESLint 9、Prettier 3、Maven 3.9、npm |

## 目录结构

```text
.
├── backend/                  Spring Boot 后端
│   ├── src/main/java/com/example/tax/
│   │   ├── auth/ user/ role/ taxpayer/ declarationtype/
│   │   ├── formtemplate/ declaration/ review/ statistics/
│   │   ├── search/ audit/ common/ config/ security/
│   │   └── TaxApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-test.yml
│   │   ├── db/migration/V1__init_schema.sql
│   │   ├── db/migration/V2__seed_demo_data.sql
│   │   └── mapper/*.xml
│   └── src/test/java/com/example/tax/
├── frontend/                 Vue 3 前端
│   ├── src/api/ components/ layouts/ router/ stores/ styles/ types/ utils/ views/
│   ├── eslint.config.js
│   └── .prettierrc.json
├── docs/                     设计、接口、测试、调研与截图
├── 开发文档/                 课程原始需求
├── .env.example
└── README.md
```

## 快速开始

### Docker 一键运行（推荐给他人直接体验）

安装并启动 Docker Desktop 后，在项目根目录双击 `START.bat`（或 `docker-start.bat`），或执行：

```powershell
docker compose up -d --build
```

启动完成后访问 <http://localhost:18080>。使用独立端口以避开本机 Spring Boot / Tomcat 常用的 `8080`。详细说明、端口配置、停止与数据重置方法见 [DOCKER.md](DOCKER.md)。

登录页包含“账号登录 / 注册账号 / 角色说明”三个并列页签。注册成功后返回登录页并预填账号。

### 1. 环境要求

- JDK 17 或更高（编译目标 Java 17）
- Maven 3.9+
- MySQL 8.0+（本机验证使用 9.0.1）
- Node.js 20+ 与 npm

### 2. 初始化数据库

使用 MySQL 管理员账号执行（密码请自行替换，不要提交到 Git）：

```sql
CREATE DATABASE tax_business
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE tax_business_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'tax_app'@'localhost' IDENTIFIED BY 'ChangeMe_Local_2026';
GRANT ALL PRIVILEGES ON tax_business.* TO 'tax_app'@'localhost';
GRANT ALL PRIVILEGES ON tax_business_test.* TO 'tax_app'@'localhost';
FLUSH PRIVILEGES;
```

表结构与种子数据由 Flyway 在后端首次启动时自动执行，无需手工导入 SQL。

### 3. 启动后端

```bash
cd backend
export DB_USERNAME=tax_app
export DB_PASSWORD=ChangeMe_Local_2026
mvn spring-boot:run
```

Windows PowerShell：

```powershell
$env:DB_USERNAME="tax_app"
$env:DB_PASSWORD="ChangeMe_Local_2026"
mvn spring-boot:run
```

启动成功后：

- 健康检查：<http://localhost:8080/actuator/health>
- OpenAPI 文档：<http://localhost:8080/swagger-ui.html>

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 <http://127.0.0.1:5173>。Vite 已将 `/api` 代理到 `http://localhost:8080`。

如需指向其他后端地址，创建 `frontend/.env.local`：

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### 5. 演示账号

密码均为 `Demo@1234`（仅用于本地教学演示）。

| 账号 | 角色 | 可见菜单 | 说明 |
| ---- | ---- | -------- | ---- |
| `admin` | ADMIN | 全部 11 项 | 用户、角色、全部业务与日志 |
| `clerk` | CLERK | 工作台、纳税人、类型、模板、申报、查询、统计 | 只能看到本人创建的申报 |
| `reviewer` | REVIEWER | 含审核管理 | 审核申报，不可审核自己创建的申报 |
| `analyst` | ANALYST | 工作台、纳税人、类型、申报、查询、统计 | 只读业务数据与统计 |
| `viewer` | VIEWER | 工作台、纳税人、类型、申报、统计 | 只读，无新建/导出按钮 |

## 演示流程

1. 使用 `clerk` 登录 → 申报管理 → 新建申报 → 选择纳税人、申报类型、期间 → 填写动态表单 → **保存并提交**。
2. 退出，使用 `reviewer` 登录 → 审核管理 → 待审核申报 → 查看详情 → **通过**（或填写原因后退回）。
3. 使用 `clerk` 查看申报状态与审核记录；被退回的申报可编辑后重新提交。
4. 使用 `admin` 查看工作台、统计分析、综合查询与操作日志；在综合查询中导出 CSV。
5. 使用 `viewer` 验证菜单裁剪、按钮隐藏、直接访问 `/users` 跳转 403、接口调用返回 403。

## 测试

### 后端自动化测试

```bash
cd backend
export TEST_DB_USERNAME=tax_app
export TEST_DB_PASSWORD=ChangeMe_Local_2026
mvn test
```

> 测试会启动 Spring 上下文并执行 `flyway.clean()` + `flyway.migrate()` 重建 **`tax_business_test`**。不要把 `TEST_DB_URL` 指向开发库或生产库。

最近一次执行结果：**28 个测试，0 失败，0 错误**。覆盖登录/停用、自助注册、重复账号、弱密码、权限、重复编号、草稿、必填校验、提交/重复提交、审核通过/退回/重复审核/自审、编辑限制、撤回、统计一致性、分页筛选、导出与权限、日志脱敏。详见 `docs/test-plan.md`。

### 前端检查

```bash
cd frontend
npm run lint
npm run format:check
npm run build
```

### 浏览器验收

验收截图位于 `docs/screenshots/`，覆盖 1366×768 与 1440×900、登录、工作台、全部管理页、申报–审核闭环、CSV 导出与 403 页面。测试结论与未验证项见 `docs/test-plan.md`。

## 配置项

| 环境变量 | 默认值 | 说明 |
| -------- | ------ | ---- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_URL` | `jdbc:mysql://localhost:3306/tax_business?...` | 开发库连接 |
| `DB_USERNAME` | `root` | 开发库账号 |
| `DB_PASSWORD` | 空 | 开发库密码（不要提交） |
| `TEST_DB_URL` | `jdbc:mysql://localhost:3306/tax_business_test?...` | 测试库连接 |
| `TEST_DB_USERNAME` | `tax_app` | 测试库账号 |
| `TEST_DB_PASSWORD` | 空 | 测试库密码 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | 允许的前端来源 |
| `EXPORT_MAX_ROWS` | `5000` | CSV 导出行数上限 |
| `VITE_API_BASE_URL` | `/api/v1` | 前端接口前缀（走 Vite 代理时无需设置） |

示例见 `.env.example`。实际 `.env`、数据库密码、日志不得提交到 Git。

## 重置演示数据

Flyway 只执行一次迁移。需要恢复初始 8 条申报等种子数据时：

```sql
DROP DATABASE tax_business;
CREATE DATABASE tax_business CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

重启后端，Flyway 会重新执行 `V1`、`V2`。也可在 MySQL 中删除 `flyway_schema_history` 后重建库，但不建议在共享环境直接 `repair`。

## 常见问题

| 现象 | 处理 |
| ---- | ---- |
| `Port 8080 was already in use` | 结束占用进程：`netstat -ano \| findstr :8080`，再 `taskkill /PID <pid> /F` |
| `Access denied for user 'tax_app'` | 检查用户、密码与 `GRANT`；确认 `DB_USERNAME/DB_PASSWORD` 已导出到当前终端 |
| Flyway `Validate failed` / checksum 不一致 | 开发库结构被手工修改或迁移文件被改动；恢复文件，或按“重置演示数据”重建库 |
| 登录后立即 401 | 确认前端使用 `withCredentials`、Vite 代理正常；会话 Cookie 为 `SameSite=Lax`，需同站访问 |
| 接口 403 | 当前角色无对应权限；这是后端安全边界，不是前端故障。用 `admin` 验证或调整角色权限 |
| 测试库数据被清空 | 预期行为：测试启动执行 `clean + migrate`；请确保 `TEST_DB_URL` 指向专用测试库 |
| Flyway 提示 MySQL 9.0 超出测试范围 | 官方支持到 8.1；本项目在 9.0.1 上迁移与测试均成功，可按需升级 Flyway |

## 已实现 / 未实现

**已实现**：上述十个模块（含工作台）、会话认证、自助注册与后端鉴权、模板版本、申报状态机、审核事务、统计聚合、组合查询与 CSV 导出、审计日志、Flyway 迁移、28 个集成测试、前端全部页面与响应式布局。

**未实现或仅基础实现**：

- 字典表已建表并有种子数据，但没有字典管理界面/API。
- 无附件上传、Excel 导入、消息通知、真实税额计算。
- 无生产部署脚本、HTTPS/反向代理配置。
- 自动化浏览器测试仅覆盖 Chromium；无压力/并发基准测试。
- 前端构建存在大 chunk 提示，未做进一步拆包。

## 文档索引

- [开发基线](docs/development-document-v1.0.md)
- [UI 调研记录](docs/ui-reference-research.md)
- [API 规范](docs/api-spec.md)
- [数据库设计（含 ER 图）](docs/database-design.md)
- [测试计划与执行记录](docs/test-plan.md)
- [变更记录](docs/changelog.md)
- [验收截图](docs/screenshots/)
