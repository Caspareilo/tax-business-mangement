# 测试计划与执行记录（V1.0）

> 执行日期：2026-10-29
> 环境：Windows 11、JDK 23（编译目标 Java 17）、Maven 3.9.14、MySQL 9.0.1、Node.js 24.13.0、Chromium 1248（Playwright 1.64.1）
> 结论：后端 26 个自动化测试全部通过；浏览器端到端检查全部通过。未验证项见第 6 节。

## 1. 测试范围与策略

| 层级 | 工具 | 覆盖 |
| ---- | ---- | ---- |
| 集成测试 | JUnit 5 + Spring Boot Test + MockMvc | 认证、授权、申报状态机、审核、统计、查询、导出、审计 |
| 数据库 | Flyway `clean + migrate`（仅测试库） | 每次测试从 V1/V2 固定种子开始，测试间事务回滚 |
| 前端构建 | `vue-tsc --noEmit && vite build` | 类型检查与生产构建 |
| 浏览器验收 | Playwright（Chromium，1366×768 / 1440×900） | 真实登录、页面渲染、CRUD 弹窗、申报–审核闭环、权限边界 |

测试库配置：`application-test.yml`，默认库 `tax_business_test`，账号 `tax_app`，密码由 `TEST_DB_PASSWORD` 注入。
`TestDatabaseConfig` 在测试上下文启动时执行 `flyway.clean()` + `flyway.migrate()`，**会清空测试库**；不要指向开发库。

## 2. 后端自动化测试

### 2.1 执行命令

```bash
cd backend
TEST_DB_PASSWORD=<本地测试库密码> mvn test
```

### 2.2 执行结果

```text
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| 测试类 | 数量 | 结果 |
| ------ | ---- | ---- |
| `AuthAndPermissionTest` | 6 | 通过 |
| `DeclarationFlowTest` | 11 | 通过 |
| `StatisticsAndSearchTest` | 5 | 通过 |
| `TaxpayerAndTemplateTest` | 4 | 通过 |

### 2.3 需求要求的 15 项测试映射

| # | 要求 | 对应用例 | 关键断言 | 结果 |
| - | ---- | -------- | -------- | ---- |
| 1 | 正确登录、错误密码、停用用户 | `loginSucceedsWithValidCredentials`、`loginFailsWithWrongPassword`、`inactiveUserCannotLogin` | 200 且 `/auth/me` 返回权限；401；停用账号 401 | 通过 |
| 2 | 经办人访问用户管理 API 返回 403 | `clerkCannotAccessUserManagement` | `GET /users` → 403 | 通过 |
| 3 | 重复纳税人编号创建失败 | `duplicateTaxpayerCodeIsRejected` | 409，`code=CONFLICT` | 通过 |
| 4 | 草稿保存并可重新读取 | `createDraftPersistsAndIsReadable` | 响应状态 `DRAFT`，`GET` 可读 | 通过 |
| 5 | 缺少必填字段时拒绝提交 | `submitRejectsMissingRequiredFormFields` | 直接写入 `{}` 后提交 → 400 | 通过 |
| 6 | 合法提交后变为待审核 | `submitMovesToPendingAndCannotSubmitTwice` | `PENDING_REVIEW`；再次提交 409 | 通过 |
| 7 | 重复提交不产生重复申报 | 同上 + `duplicateDeclarationForSamePeriodIsRejected` | 状态冲突 409；同条件有效申报仅 1 条 | 通过 |
| 8 | 审核通过后状态与历史一致 | `reviewerCanApproveAndHistoryIsRecorded` | `APPROVED`；历史 `action=APPROVE`，审核人正确 | 通过 |
| 9 | 退回必须填写原因 | `returnRequiresCommentThenMovesToReturned` | 空意见 400；填写后 `RETURNED` | 通过 |
| 10 | 经办人不能审核自己的申报 | `reviewerCannotApproveOwnDeclaration` | 403 | 通过 |
| 11 | 已处理申报重复审核冲突 | `approvingProcessedDeclarationReturnsConflict` | 第二次审核 409 | 通过 |
| 12 | 统计与筛选业务数据一致 | `dashboardTotalsMatchSearchTotals` | 总量/通过数与列表 `total` 一致 | 通过 |
| 13 | 分页和组合筛选正确 | `searchSupportsKeywordAndPagination`、`taxpayerListFiltersAndPaginates` | 关键词总数、分页条数、类型筛选正确 | 通过 |
| 14 | 导出遵守筛选与权限 | `exportReturnsCsvAndWritesAuditLog`、`viewerCannotExportButCanSearch` | BOM+表头、仅含筛选结果、日志 1 条；只读导出 403 | 通过 |
| 15 | 日志不含密码与 Token | `auditLogsNeverContainPasswords` | 响应不含 `Demo@1234`、`"password"` | 通过 |

### 2.4 额外覆盖

- 未登录访问业务 API → 401（`unauthenticatedRequestReturns401`）。
- VIEWER 创建纳税人 → 403（`viewerCannotCreateTaxpayer`）。
- 已提交/已通过申报不可编辑（两条用例）。
- 待审核申报可撤回 → `WITHDRAWN`。
- 模板无字段发布 → 400，提示“至少需要一个启用字段”。
- 已发布模板原位修改 → 409。
- 退回后状态为 `RETURNED`，退回意见写入审核记录。

## 3. 前端静态检查与构建

```bash
cd frontend
npm install
npm run lint
npm run format:check
npm run build
```

结果（2026-10-29）：

- `eslint .`：0 错误、0 警告。
- `prettier --check`：全部文件符合格式。
- `vue-tsc --noEmit`：通过。
- `vite build`：构建成功，仅有 chunk 体积提示，无类型或编译错误。

## 4. 浏览器端到端验收

执行方式：Playwright 1.64.1（Chromium headless），前端 `http://127.0.0.1:5173`，后端 `http://127.0.0.1:8080`，通过 Vite 代理调用真实接口。
截图存放：`docs/screenshots/`（均为实际运行截图，未合成、未伪造）。

### 4.1 场景与结果

| 场景 | 检查点 | 结果 |
| ---- | ------ | ---- |
| 登录页 1366×768 | 品牌区、登录表单、演示账号快捷填充 | 通过 |
| 管理员登录 | KPI 为数据库实时值（非 `-`）、3 个图表画布 | 通过 |
| 工作台 1440×900 | 布局不溢出、图表自适应 | 通过 |
| 用户管理 | 页面标题、新建用户弹窗 | 通过 |
| 角色权限 | 权限分配弹窗（菜单/API 分组） | 通过 |
| 纳税人 / 申报类型 | 列表渲染、筛选区 | 通过 |
| 表单模板 | 模板预览抽屉 | 通过 |
| 申报管理 | 列表、新建弹窗、动态表单渲染 | 通过 |
| 申报闭环（CLERK） | 选择未占用组合 → 填写必填项 → 保存并提交，接口 200，列表出现“待审核” | 通过 |
| 审核闭环（REVIEWER） | 待办出现新申报 → 填写意见 → 通过，接口 200，行移出待办 | 通过 |
| 审核历史 | 历史列表有记录 | 通过 |
| 综合查询 / 导出 | CSV 下载，UTF-8 BOM、表头正确、受当前筛选约束 | 通过 |
| 统计分析 | 4 个图表画布 + 类型明细，数据来自接口；趋势范围 6/12 个月切换与重置 | 通过 |
| 操作日志 / 个人设置 | 页面与数据渲染 | 通过 |
| 只读用户 | 菜单 5 项（管理员 11 项）；无“新建申报”“导出 CSV”按钮 | 通过 |
| 后端权限边界 | 只读用户访问 `/users` → 前端 `/403`；`fetch('/api/v1/users')` → 403；已登录读取纳税人 → 200 | 通过 |
| 浏览器控制台 | 无 JS 异常；仅记录 1 条预期的 403 资源错误（权限边界验证） | 通过 |

### 4.2 验收过程中的修复

- 修复趋势图图例与右轴名称重叠（图例改为居中）。
- 1366×768 下申报列表金额列被固定操作列遮挡：精简非关键列并调整列宽。
- 动态表单长标签换行：标签宽度调整为 150px，单选字段独占整行。
- 期间列日期换行：统一 `.period-text` 不换行、12px 等宽数字。
- 工作台类型图表改为按模拟金额绘制，补充金额/条数提示；统计页新增近 6/12 个月趋势范围选择。

## 5. 可重复性说明

- 后端测试通过 `Testing` 配置自动重建测试库，可重复执行；断言基于固定种子（8 条申报）。
- 浏览器验收会创建真实演示数据（草稿→待审核→通过）。脚本按“未占用的纳税人+期间”组合选择数据，可重复执行，但会在开发库累积记录；需要干净演示库时按 README 重置。
- UI 验收脚本为临时执行脚本，未纳入仓库；截图与结果可核验。

## 6. 未验证项与限制

1. 仅使用 Chromium 验证，未覆盖 Firefox/Safari；未做移动端真机测试。
2. 未做压力/并发基准测试；“并发重复审核”通过条件 UPDATE 与 409 逻辑测试，但未使用多线程真实并发压测。
3. 未部署到独立生产环境；未验证 HTTPS、反向代理、跨域生产配置。
4. MySQL 9.0.1 下 Flyway 提示“版本新于已测试范围（8.1）”，迁移与测试均成功，但该组合非官方测试矩阵。
5. 审计日志敏感值检查针对接口返回与日志表内容；未对全量应用日志文件做自动化脱敏扫描。
6. 表格导出仅 CSV；未实现 Excel/PDF。
7. 前端构建存在大 chunk 警告（ECharts 与 Element Plus），未做进一步拆包优化。
