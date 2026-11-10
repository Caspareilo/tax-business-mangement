# 变更记录

## V1.4 - 2026-11-10

### 课程技术基线统一

- 本地注册的 `/hello` Servlet 已加入传统项目，用于实验一生命周期和中文响应验证。
- Struts 控制层固定为 2.5.33，Servlet/JSP API 固定为 Java EE 8 的 `javax.servlet` 体系。
- Docker 构建与运行 JDK 固定为 Eclipse Temurin 11，Tomcat 固定为 9.0.123，MySQL 固定为 8.0.46。
- 新增 `scripts/verify-baseline.ps1`，自动检查 WAR 打包、JDK、Tomcat、MySQL、`javax.servlet` 和运行依赖。
- 新增 `docs/version-baseline.md`，记录最终版本基线与验证命令。
- 课程仓库只发布传统 Java EE 8 项目，不再发布 Spring Boot 3 / Vue 3 实现。
- AuthFilter 对浏览器扩展注入的 `/hybridaction/` 统计请求返回 204，避免误触发业务 404 页面。

### 验证

- `mvn clean test`：30 个测试全部通过。
- `mvn clean package`：WAR 构建成功。
- 本地 Tomcat 9.0.123 部署后，`/hello?name=吴学孟` 返回 HTTP 200。

## V1.3 — 2026-11-03

### 登录页自助注册

- 登录页新增“注册账号”页签，与“账号登录”“角色说明”在桌面和移动端保持同一行等宽对齐。
- 移除登录按钮下方的重复注册提示，注册入口只保留在页签栏。
- 新增 `RegisterAction`、`/auth/register.action` 和公开注册白名单。
- 注册采用服务端账号格式、密码强度、确认密码和重复账号校验。
- 注册事务创建用户并固定分配 `VIEWER` 只读角色，禁止客户端指定高权限角色。
- 注册成功后返回登录页并预填新账号。

### 验证

- 后端测试增至 30 个，全部通过。
- 登录页桌面与手机尺寸视觉检查通过，无横向溢出。
- 重新生成登录、注册、工作台、申报、审核、纳税人、统计和查询等最新运行截图。
- README 第一行使用大号加粗标题突出部署文档入口，并直接展示最新登录与工作台截图。

## V1.2 — 2026-11-02

### Struts 迁移

- Web 入口由 14 个 `HttpServlet` 迁移为 14 个 Struts Action，保留 Service/DAO/JSP 分层。
- 新增 `BaseAction`、`struts.xml`，按 `/auth`、`/declarations`、`/admin/users` 等命名空间配置 Action 和 JSP Result。
- 业务入口统一为 `.action`，同步更新 JSP 链接、表单、分页与内部 PRG 重定向。
- 保持 Servlet 4.0 / JSP 2.3 / JDBC / MySQL 传统技术栈，固定使用 `javax.servlet`，不升级 Struts 7 或 Tomcat 10+。
- 禁止动态方法调用和静态方法访问，并防止客户端参数覆盖 Struts 路由 `operation`。

### Docker 一键启动

- 新增 `Dockerfile`、`compose.yaml`、`.env.docker.example`。
- 新增 `START.bat`、`docker-start.bat`、`docker-stop.bat`、`docker-logs.bat` 与 PowerShell 启动脚本。
- Docker 默认入口为 <http://localhost:18082>。
- MySQL 首次启动自动导入 schema 与演示数据，数据库使用命名卷持久化。
- Docker 构建使用 Maven cache mount；应用使用 Tomcat 9.0.123，健康检查覆盖 Struts 登录页。

### 验证

- `mvn test`：28 个测试全部通过。
- Docker MySQL 8 实库初始化成功，Tomcat 9 / Struts 登录与五个角色入口正常。
- 管理员、经办人、审核员、分析师、只读用户共 23 个页面场景返回 200；CSV 导出正常。
- 容器重启后登录页恢复；客户端 `operation` 参数无法覆盖服务端 Struts 路由。
- Maven 运行依赖中不存在 Jakarta Servlet/JSP API，源码与容器均使用 `javax.servlet`。

## V1.1 — 2026-11-01

### UI 改造

- 按 Ant Design Pro Analysis Dashboard 参考图重构统一后台布局：白色顶栏、浅色侧栏、浅灰内容区和白色业务面板。
- Dashboard 调整为四列核心指标、可切换数量/金额趋势、真实类型金额排名、最近申报与状态环图。
- 统一业务列表、筛选区、表单、详情摘要、流程时间线、分页、状态标签和错误页的视觉规范。
- 侧栏支持分组展开/收起；移动端支持遮罩抽屉和 Esc 关闭。
- 登录页按参考 JSP 的左右分栏与页签风格重构，并增加五个角色的测试账号一键登录区。
- 新增本地登录插画与 favicon，不引入额外第三方资源。
- 增加 UI 改造记录与真实前后对比截图，见 `docs/ui-redesign-v1.1.md` 和 `docs/ui-before-after/`。

## V1.0 — 2026-10-29

### 新增

- 完成传统 Java EE 版税务业务管理系统九大模块：
  - 工作台（KPI、趋势、状态分布、类型金额、最近申报）
  - 用户 / 角色 / 权限管理
  - 纳税人档案
  - 申报类型
  - 表单模板、动态字段、预览、版本管理
  - 申报草稿 / 提交 / 撤回 / 退回重提
  - 审核通过 / 退回 / 审核历史
  - 统计分析（6/12 个月趋势、状态、类型、主体类型）
  - 综合查询、CSV 导出、操作审计、数据字典、个人中心
- 建立 `database/schema.sql`（13 张表）与 `database/seed-demo.sql`（虚构演示数据）。
- 建立 JUnit 5 + H2（MySQL 模式）测试体系，共 9 个测试类、28 个用例。
- 增加登录、权限、CSRF、会话轮换、BCrypt、审计脱敏等安全控制。
- 增加 CSV 导出公式注入防护与 5000 行上限。
- 增加 Tomcat 9 部署验证与 11 张界面截图（`docs/screenshots/`）。
- 增加开发文档、UI 调研、数据库设计、路由清单、测试计划、README。

### 修复

- 修复 JSP 静态包含片段（`footer.jspf`、`sidebar.jspf`、`topbar.jspf`、`messages.jspf`、`pagination.jspf`）未声明 `pageEncoding` 导致的中文二次编码乱码。
- 修复仪表盘与统计页未加载 `chart.umd.min.js` 导致图表空白。
- 为侧边栏当前菜单补充 `aria-current` 属性。
- 修正角色权限编辑页缺少权限分组名称的问题（`sys_permission.group_name` 及 DAO 映射）。
- 修正 `CsvUtil.escape` 可见性，便于单元测试直接验证转义规则。
- 修复 `fmt:formatNumber` 在请求缺少 `Accept-Language` 时退回原始数值、丢失千分位的问题：在 `head.jspf` 固定格式化区域为 `zh_CN`。
- 统计按数据范围过滤：经办人/只读用户只统计本人创建的申报，分析师/审核员/管理员统计全量，并新增 STAT-02 覆盖。
- 退出登录的 GET 请求改为仅重定向，不销毁会话，确保 GET 不产生状态变更。
- 测试用例期间范围调整为已结束月份，避免“期间结束日期晚于今天”的校验失败。

### 验证

- `mvn clean test`：28 个测试全部通过，0 失败、0 错误、0 跳过。
- `mvn clean package`：生成 `target/tax-business-system.war`。
- Tomcat 9.0.123 实际部署：登录、工作台、申报、审核、统计、查询、系统管理页面均返回预期状态码；越权 403；CSRF 缺失 403；SQL 注入登录失败。
- 使用 Chromium headless 完成 1440px 视口截图，图表与中文渲染正常。

### 已知限制

- 本地非 Docker 模式需要自行提供 MySQL 8 账号；Docker 模式自动初始化 MySQL 8.0.46。
- 未做多浏览器、移动端真机、性能压测与专业渗透测试。
- 未接入真实税务接口、邮件或短信服务。
- 错误摘要暂未实现 GOV.UK 式的字段锚点跳转与自动聚焦，仅提供顶部提示与字段内联错误。
