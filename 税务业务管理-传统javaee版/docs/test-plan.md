# 测试计划与执行结果

- 版本：V1.0
- 测试日期：2026-10-29
- 测试负责人：项目开发者
- 结论：自动化测试 28/28 通过；Tomcat 9 实际部署验证通过；MySQL 实库验证因本机缺少可用账号密码未执行。

## 1. 测试目标

1. 验证登录、角色权限、CSRF、会话失效等安全边界。
2. 验证申报草稿、提交、撤回、退回重提的状态机与重复申报约束。
3. 验证审核通过/退回、审核意见必填、自审拦截、并发冲突。
4. 验证统计口径、组合查询、CSV 导出与公式注入防护。
5. 验证审计日志不泄露密码、Cookie、Session ID 等敏感信息。
6. 验证 WAR 能在 Tomcat 9（`javax.servlet`）实际启动，JSP 正常编译与渲染。

## 2. 测试环境

| 项目 | 实际值 |
| --- | --- |
| 操作系统 | Windows 11 Home China 10.0.26200 |
| JDK | 23.0.2（`maven.compiler.release=11`，字节码目标 Java 11） |
| Maven | 3.9.14 |
| 构建产物 | `target/tax-business-system.war`（约 5.0 MB） |
| 应用服务器 | Apache Tomcat 9.0.123 |
| 生产数据库 | MySQL 9.0.1 已安装、服务运行；**本机无可用账号密码，未执行建库** |
| 自动化测试数据库 | H2 2.2.224，`MODE=MySQL;DATABASE_TO_LOWER=TRUE`，每个测试新建内存库 |
| 浏览器 | Chromium 1248（Playwright 驱动，headless），1440px 视口 |
| 端口 | Tomcat 验证实例 18080（避免与本机 8080 冲突） |

说明：H2 仅用于测试和本机部署验证，生产脚本仍是 MySQL 8 语法。H2 脚本把 `JSON` 列映射为 `TEXT`，DAO 使用字符串绑定，因此业务代码无需分叉。

## 3. 测试策略

| 层级 | 方法 | 覆盖 |
| --- | --- | --- |
| 单元/服务集成 | JUnit 5 + H2 内存库，每个测试重建 schema + seed | Service、DAO、权限、事务、审计 |
| 页面/路由 | curl 携带真实会话 Cookie，检查状态码与页面标记 | 登录、权限、CSRF、PRG、404/403 |
| 浏览器 | Playwright + Chromium，真实填表登录并截图 | 页面渲染、图表、表单、响应式基线 |
| 部署 | Tomcat 9.0.123 + WAR 实际启动 | JSP 编译、静态资源、启动日志 |
| 手工安全 | SQL 注入登录、停用账号、GET 退出、缺少 CSRF | 关键安全路径 |

## 4. 自动化测试结果

执行命令：

```bash
mvn clean test
```

实际结果（Surefire 报告）：

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
```

| 测试类 | 用例数 | 覆盖点 |
| --- | ---: | --- |
| `AuthServiceTest` | 4 | 登录成功加载角色/权限；密码错误写审计且不含密码；停用账号拒绝；未知账号统一错误 |
| `PermissionServiceTest` | 4 | VIEWER 不能保存草稿；VIEWER 不能审核；ADMIN 不含 `review:manage`；CLERK 不能管理用户 |
| `TaxpayerServiceTest` | 3 | 编号重复拒绝且数量不变；新增后可检索；停用纳税人不能新建申报 |
| `DeclarationServiceTest` | 4 | 草稿快照与金额汇总；提交后进入待审核且重复期间被拒；撤回与已通过不可编辑；退回后可改并重提 |
| `ReviewServiceTest` | 4 | 通过并写审核记录；退回意见必填；审核人不能审核自己的申报；过期版本审核被拒 |
| `StatisticsServiceTest` | 2 | 汇总口径（12 条、1,678,000.00、通过率 66.7%）；经办人仅统计本人创建的申报 |
| `SearchServiceTest` | 4 | 组合筛选；关键字；经办人数据范围；导出权限与行数 |
| `AuditServiceTest` | 2 | 登录成功/失败均留痕，日志不含密码/Session/Cookie；筛选 |
| `CsvUtilTest` | 1 | 逗号/引号/换行转义，`= + - @` 公式注入前缀 |

### 4.1 关键用例明细

| 编号 | 前置 | 操作 | 预期 | 实际 |
| --- | --- | --- | --- | --- |
| AUTH-01 | 种子数据 | `admin/Admin@123` 登录 | 成功，加载角色与 14 个权限 | 通过 |
| AUTH-02 | 种子数据 | 错误密码登录 | 失败；审计记录存在且不含明文密码 | 通过 |
| AUTH-03 | `disabled1` | 正确密码登录 | 拒绝，提示账号已停用 | 通过 |
| AUTH-04 | 不存在账号 | 登录 | 与密码错误相同文案，避免账号枚举 | 通过 |
| PERM-01 | VIEWER 登录 | 保存草稿 | `AccessDeniedException` | 通过 |
| PERM-02 | VIEWER 登录 | 审核通过 | 拒绝 | 通过 |
| PERM-03 | ADMIN 登录 | 审核 | 拒绝（管理员无审核权限） | 通过 |
| PERM-04 | CLERK 登录 | 管理用户 | 拒绝 | 通过 |
| TAX-01 | 已有编号 | 新建同编号纳税人 | 拒绝，总数仍为 6 | 通过 |
| TAX-02 | 无 | 新建后按名称搜索 | 命中 1 条 | 通过 |
| TAX-03 | 停用纳税人 3 | 新建申报 | 拒绝，`taxpayerId` 有字段错误 | 通过 |
| DECL-01 | CLERK | 保存草稿 | 状态 DRAFT；名称/类型/V1 快照；金额 120000.00 | 通过 |
| DECL-02 | 已提交同期间 | 再次提交 | 拒绝“不能重复提交” | 通过 |
| DECL-03 | 已提交/已通过 | 撤回、编辑 | 待审核可撤回；已通过不可编辑/撤回 | 通过 |
| DECL-04 | 已退回申报 | 修改并重提 | 状态回到待审核，version 增加 | 通过 |
| REVIEW-01 | 待审核申报 | 通过 | 状态 APPROVED，审核记录 1 条 | 通过 |
| REVIEW-02 | 待审核申报 | 退回且意见为空 | 校验失败 | 通过 |
| REVIEW-03 | 审核人=创建人 | 审核 | 拒绝自审 | 通过 |
| REVIEW-04 | 旧 version | 审核 | 条件更新 0 行，拒绝 | 通过 |
| STAT-01 | 种子数据 | 统计 | 12 条；2/3/2/4/1；金额 1,678,000.00；通过率 66.7% | 通过 |
| STAT-02 | 插入 admin 创建的申报 | 分别以 admin、clerk 统计 | admin 13 条；clerk 仍 12 条（看不到他人数据） | 通过 |
| SEARCH-01 | 种子数据 | 组合筛选 | 返回符合条件集合 | 通过 |
| SEARCH-02 | 关键字 | 查询 | 命中预期 | 通过 |
| SEARCH-03 | CLERK | 查询 | 仅本人数据，12 条 | 通过 |
| SEARCH-04 | ANALYST | 导出 | 有权限，返回当前筛选 2 行 | 通过 |
| AUDIT-01 | 登录成功/失败 | 查日志 | 均有记录；无密码、Session ID、Cookie | 通过 |
| AUDIT-02 | 种子日志 | 按类型/结果筛选 | 结果正确 | 通过 |
| SEARCH-05 | 特殊字符 | CSV 转义 | 引号加倍、公式前缀 `'` | 通过 |

## 5. Tomcat 9 部署验证

### 5.1 部署步骤（实际执行）

1. 下载 `apache-tomcat-9.0.123.zip`，`unzip -tq` 校验无错误，解压到独立目录。
2. `mvn clean package` 生成 WAR。
3. 将 WAR 部署为 `webapps/ROOT.war`，使用 H2 文件库作为验证数据库（系统属性注入，未改代码）。
4. 修改验证实例端口为 18080，启动 Tomcat。
5. 日志出现 `Server startup in [2516] milliseconds`，无 `SEVERE`。

### 5.2 页面与权限结果

| 请求（admin） | 状态码 | 结果 |
| --- | ---: | --- |
| `/auth/login` | 200 | 登录页正常，CSRF 隐藏域存在 |
| `POST /auth/login` | 302 | 跳转 `/dashboard`，下发新 `JSESSIONID`（HttpOnly） |
| `/dashboard` | 200 | KPI、趋势图、状态环图、类型金额图正常渲染 |
| `/declarations` | 200 | 列表、状态徽标、分页正常 |
| `/declarations/create` | 200 | 动态字段按模板渲染 |
| `/search/declarations` | 200 | 组合筛选表单正常 |
| `/statistics/overview` | 200 | 4 张图表与状态明细正常 |
| `/taxpayers` | 200 | 列表正常 |
| `/declaration-types` | 200 | 列表正常 |
| `/form-templates` | 200 | 列表正常 |
| `/admin/users` | 200 | 用户列表正常 |
| `/admin/roles` | 200 | 权限分组勾选正常 |
| `/admin/audit-logs` | 200 | 日志列表正常 |
| `/admin/dictionaries` | 200 | 字典列表正常 |
| `/profile` | 200 | 个人资料正常 |
| `/reviews/pending` | 403 | 管理员无审核权限，符合设计 |

| 角色 | 路由 | 状态码 | 结论 |
| --- | --- | ---: | --- |
| CLERK | `/admin/users` | 403 | 无用户管理权限 |
| CLERK | `/reviews/pending` | 403 | 无审核权限 |
| CLERK | `/declarations/create` | 200 | 可录入 |
| CLERK | `/statistics/overview` | 403 | 无统计权限 |
| REVIEWER | `/reviews/pending` | 200 | 可审核 |
| REVIEWER | `/admin/users` | 403 | 无系统管理权限 |
| ANALYST | `/statistics/overview` | 200 | 可看统计 |
| ANALYST | `/search/export` | 200 | 可导出 |
| ANALYST | `/declarations/create` | 403 | 不可录入 |

### 5.3 安全与状态流转验证

| 检查 | 操作 | 实际结果 |
| --- | --- | --- |
| CSRF 缺失 | 已登录 POST `/declarations/save`，不带 token | 403，未写入数据 |
| GET 退出 | GET `/auth/logout` | 302 `/dashboard`；随后 `/dashboard` 仍 200，会话未失效 |
| POST 退出 | 携带 token POST `/auth/logout` | 302 登录页，会话失效 |
| SQL 注入登录 | 用户名 `' OR 1=1 --` | 200 登录页，提示“账号或密码错误” |
| 停用账号 | `disabled1/Disabled@123` | 200，提示“账号已停用” |
| 未知路由 | `/declarations/nope` | 404 错误页 |
| 审核通过 | reviewer1 审核待审核单 | 302，状态变为已通过，待审核列表移除 |
| 审核意见中文 | 提交“资料齐全，金额核对无误。” | 详情时间线正确显示，无乱码 |
| 草稿保存 | clerk1 提交动态字段 | 302 详情页，金额与字段回显正确 |
| 提交申报 | 对草稿 POST `/declarations/submit` | 302，状态“待审核”，出现撤回按钮 |

### 5.4 部署中发现并修复的问题

1. **静态包含文件中文乱码**：`footer.jspf`、`sidebar.jspf` 等片段未声明 `pageEncoding`，Jasper 按默认编码读取导致二次编码。已在 5 个 JSP 片段首行加入 `<%@ page pageEncoding="UTF-8" %>`，重新部署后校验字节为单次 UTF-8。
2. **图表空白**：页面引用了 `charts.js`，但未加载 `chart.umd.min.js`。已在 footer 中按 `extraScript == 'charts.js'` 条件加载本地 Chart.js 4.4.3，仪表盘与统计页图表恢复。
3. **HTML 可访问性**：侧边栏当前项补充 `aria-current="page"`（非当前项为 `false`），与调研中的导航约定一致。
4. **金额千分位不稳定**：`fmt:formatNumber` 依赖请求的格式化区域，客户端未发送 `Accept-Language` 时会退回原始 `toString()`（如 `1678000.00`）。已在 `head.jspf` 固定 `<fmt:setLocale value="zh_CN"/>`，复测无 `Accept-Language` 与带 `zh-CN` 请求均输出 `1,678,000.00`。

## 6. 未执行项与限制

| 项目 | 状态 | 原因/后续 |
| --- | --- | --- |
| MySQL 8 实库建表与查询 | 未执行 | 本机 MySQL 无可用账号密码；禁止猜测凭据。获得授权账号后执行 `schema.sql`、`seed-demo.sql` 并跑冒烟 |
| MySQL 执行计划/压测 | 未执行 | 无实库；当前仅按查询条件设计索引 |
| 多浏览器兼容 | 部分 | 仅 Chromium headless；Bootstrap 5 基线支持现代浏览器，未测 Safari/Firefox |
| 移动端真机 | 未执行 | 仅 1440px 视口截图；CSS 含窄屏断点，未做真机回归 |
| 渗透测试 | 未执行 | 已做基础安全用例，未做专业渗透 |
| 邮件/短信/真实税局接口 | 不涉及 | 课程项目不接外部系统 |

## 7. 退出标准

- [x] 所有自动化测试通过（28/28）
- [x] WAR 在 Tomcat 9 启动，核心页面 200
- [x] 角色越权返回 403
- [x] CSRF 缺失被拒绝
- [x] 申报状态机与审核规则有测试覆盖
- [x] 审计日志不含敏感信息
- [ ] MySQL 实库验证（待凭据）
