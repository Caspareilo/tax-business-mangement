# 路由与接口清单

本项目是传统 Java EE 服务端渲染应用，没有独立 REST API，路由和结果由 `struts.xml` 定义。下表中的“接口”指 Struts Action 路由（.action）：GET 返回 HTML 页面，POST 处理表单并重定向（Post/Redirect/Get）。

- 应用上下文：`ctx`（WAR 名为 `tax-business-system.war` 时通常为 `/tax-business-system`；部署为 `ROOT.war` 时为空）
- 所有 POST 请求必须携带 `csrfToken`（登录页由服务端在会话中生成并写入隐藏域）
- 所有响应默认 `text/html;charset=UTF-8`；CSV 导出为 `text/csv;charset=UTF-8`
- 受保护路由由 `AuthFilter` 统一拦截：未登录 GET 重定向登录页，未登录非 GET 返回 401；权限不足返回 403
- 业务级权限（数据归属、审核人不能审核自己的申报等）在 Service 层再次校验

## 1. 认证与个人中心

| 方法 | 路径 | 权限 | 说明 | 成功响应 |
| --- | --- | --- | --- | --- |
| GET | `/auth/login.action` | 匿名 | 登录页；已登录用户直接跳转工作台 | 200 HTML |
| POST | `/auth/login.action` | 匿名 + CSRF | 校验账号密码；停用账号拒绝；成功后轮换 Session ID | 302 `/dashboard.action` |
| GET | `/auth/register.action` | 匿名 | 打开登录页并定位“注册账号”页签 | 200 HTML |
| POST | `/auth/register.action` | 匿名 + CSRF | 创建只读账号并固定分配 `VIEWER` 角色 | 302 登录页并预填账号 |
| POST | `/auth/logout.action` | 登录 + CSRF | 记录退出日志、`session.invalidate()` | 302 `/auth/login.action?logout=1` |
| GET | `/auth/logout.action` | 登录 | 不做状态变更，重定向工作台 | 302 `/dashboard.action` |
| GET | `/profile.action` | 登录 | 个人资料与修改密码表单 | 200 HTML |
| POST | `/profile/password.action` | 登录 + CSRF | 校验旧密码、新密码强度与确认密码 | 302 `/profile.action` |

登录失败统一提示“账号或密码错误”，不区分账号不存在与密码错误；停用账号单独提示。审计日志只记录用户名、结果、IP/追踪号，不记录密码。

注册字段为 `displayName`、`username`、`password`、`confirmPassword`。账号长度 3-64，密码至少 8 位且包含字母和数字；客户端不能提交角色字段。

## 2. 工作台

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/dashboard.action` | `dashboard:view` | KPI、近 6 个月趋势、状态分布、按类型金额、最近申报 |

普通经办人只统计自己创建的申报；审核员/分析师/管理员按各自数据范围统计。统计口径在 `StatisticsService` 中统一实现。

## 3. 申报管理

| 方法 | 路径 | 权限 | 说明 | 成功响应 |
| --- | --- | --- | --- | --- |
| GET | `/declarations.action` | `declaration:view` | 我的申报列表，支持 `keyword`、`status`、`page`、`size` | 200 HTML |
| GET | `/declarations/create.action` | `declaration:create` | 新建表单；`taxpayerId`、`typeId` 预选 | 200 HTML |
| GET | `/declarations/edit.action?id=` | `declaration:submit` + 数据归属 | 编辑草稿或已退回申报 | 200 HTML |
| GET | `/declarations/detail.action?id=` | `declaration:view` + 数据范围 | 详情、表单快照、审核进度 | 200 HTML |
| POST | `/declarations/save.action` | `declaration:create` / `declaration:submit` | 新建或更新草稿，服务端按模板校验字段 | 302 `/declarations/detail.action?id=` |
| POST | `/declarations/submit.action` | `declaration:submit` + 数据归属 | 草稿/退回 → 待审核；检查重复活动申报 | 302 详情页 |
| POST | `/declarations/withdraw.action` | `declaration:submit` + 数据归属 | 待审核 → 已撤回；已通过不可撤回 | 302 详情页 |

字段命名：固定字段 `taxpayerId`、`declarationTypeId`、`periodStart`、`periodEnd`、`id`；动态字段 `field_<field_code>`。金额字段按 `DECIMAL(18,2)` 解析，空值与非法数字返回字段级错误。

状态机：`DRAFT → PENDING_REVIEW → APPROVED | RETURNED | WITHDRAWN`，`RETURNED → PENDING_REVIEW`。已通过、已撤回不可编辑。

## 4. 审核

| 方法 | 路径 | 权限 | 说明 | 成功响应 |
| --- | --- | --- | --- | --- |
| GET | `/reviews/pending.action` | `review:manage` | 待审核列表，支持分页 | 200 HTML |
| GET | `/reviews/detail.action?id=` | `review:manage` | 审核详情：摘要、表单内容、审核历史 | 200 HTML |
| POST | `/reviews/approve.action` | `review:manage` | 通过；审核人不得为申报创建人 | 302 `/reviews/pending.action` |
| POST | `/reviews/return.action` | `review:manage` | 退回；`comment` 必填 | 302 `/reviews/pending.action` |

审核操作使用事务 + 条件更新（`WHERE status='PENDING_REVIEW' AND version=?`）。受影响行数为 0 时提示“申报状态已变化，请刷新后重试”，不产生半成品审核记录。

## 5. 统计与综合查询

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/statistics/overview.action` | `statistics:view` | 总数、通过率、状态分布、按类型/主体类型金额、月度趋势；`months=6/12` |
| GET | `/search/declarations.action` | `declaration:view` | 组合查询：关键字、纳税人、类型、状态、日期范围、经办人 |
| GET | `/search/export.action` | `declaration:view` + `export:declaration` | 导出当前筛选结果为 CSV |

CSV 规则：

- UTF-8 BOM，便于 Excel 直接打开中文
- 最多 5000 行，超出提示缩小范围
- 对以 `= + - @` 开头的单元格加前缀 `'`，防止公式注入
- 字段含逗号、引号、换行时按 RFC 4180 转义
- 导出行为写入审计日志

## 6. 纳税人

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/taxpayers.action` | `taxpayer:view` | 列表与关键字/类型/状态筛选 |
| GET | `/taxpayers/create.action` | `taxpayer:manage` | 新建表单 |
| GET | `/taxpayers/edit.action?id=` | `taxpayer:manage` | 编辑表单 |
| GET | `/taxpayers/detail.action?id=` | `taxpayer:view` | 详情与相关申报 |
| POST | `/taxpayers/save.action` | `taxpayer:manage` + CSRF | 新建/更新；编号唯一 |
| POST | `/taxpayers/status.action` | `taxpayer:manage` + CSRF | 启用/停用；停用后不能新建申报 |

## 7. 申报类型

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/declaration-types.action` | `type:manage` | 列表 |
| GET | `/declaration-types/edit.action?id=` | `type:manage` | 新建/编辑表单 |
| POST | `/declaration-types/save.action` | `type:manage` + CSRF | 保存；编码唯一 |
| POST | `/declaration-types/status.action` | `type:manage` + CSRF | 启用/停用 |

## 8. 表单模板

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/form-templates.action` | `template:manage` | 模板列表（按编码聚合展示版本） |
| GET | `/form-templates/edit.action?id=` | `template:manage` | 编辑草稿模板与字段 |
| GET | `/form-templates/preview.action?id=` | `template:manage` | 只读预览渲染效果 |
| GET | `/form-templates/versions.action?code=` | `template:manage` | 同一模板编码的版本历史 |
| POST | `/form-templates/save.action` | `template:manage` + CSRF | 保存模板与字段（仅草稿可改） |
| POST | `/form-templates/publish.action` | `template:manage` + CSRF | 发布；同编码其他已发布版本自动停用 |
| POST | `/form-templates/disable.action` | `template:manage` + CSRF | 停用模板 |
| POST | `/form-templates/new-version.action` | `template:manage` + CSRF | 基于当前版本复制出下一草稿版本 |

字段类型：`TEXT`、`INTEGER`、`AMOUNT`、`DATE`、`RADIO`、`MULTI`、`BOOLEAN`。单选/多选选项存于 `options_json`。模板发布后不可直接修改，必须新建版本；已产生的申报保留 `template_version_no` 快照。

## 9. 系统管理

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/users.action` | `user:manage` | 用户列表与筛选 |
| GET | `/admin/users/edit.action?id=` | `user:manage` | 新建/编辑用户 |
| POST | `/admin/users/save.action` | `user:manage` + CSRF | 保存用户与角色 |
| POST | `/admin/users/status.action` | `user:manage` + CSRF | 启用/停用；不能停用自己 |
| POST | `/admin/users/reset-password.action` | `user:manage` + CSRF | 重置为临时密码，返回一次性提示 |
| GET | `/admin/roles.action` | `role:manage` | 角色列表 |
| GET | `/admin/roles/edit.action?id=` | `role:manage` | 编辑角色与权限勾选 |
| POST | `/admin/roles/save.action` | `role:manage` + CSRF | 保存角色权限 |
| POST | `/admin/roles/status.action` | `role:manage` + CSRF | 启用/停用角色 |
| GET | `/admin/audit-logs.action` | `audit:view` | 审计日志，按操作类型/结果/时间/关键字筛选 |
| GET | `/admin/dictionaries.action` | `dict:manage` | 字典列表 |
| POST | `/admin/dictionaries/save.action` | `dict:manage` + CSRF | 新增/更新字典项 |
| POST | `/admin/dictionaries/status.action` | `dict:manage` + CSRF | 启用/停用字典项 |

## 10. 错误与状态码

| 状态码 | 场景 | 页面 |
| --- | --- | --- |
| 200 | 正常页面 | 对应 JSP |
| 302 | POST 成功或需要登录跳转 | `Location` 指向列表/详情/登录页 |
| 400 | 参数格式错误（如非法 ID） | 403/404/500 错误页或表单回显 |
| 401 | 未登录访问非 GET 受保护路由 | 登录页或 403 页 |
| 403 | 权限不足、CSRF 校验失败 | `error/403.jsp` |
| 404 | 未知路由/资源不存在 | `error/404.jsp` |
| 500 | 未预期异常 | `error/500.jsp`，仅显示追踪号，不输出堆栈/SQL |

错误页不包含堆栈、SQL、服务器路径；详细异常写入服务端日志并带 `traceId`，响应头 `X-Trace-Id` 可用于排查。

## 11. 请求示例

登录并获取 CSRF：

```bash
curl -c cookies.txt http://localhost:8080/tax-business-system/auth/login.action
# 从 HTML 中提取 name="csrfToken" value="..."
curl -b cookies.txt -c cookies.txt -X POST \
  -d "csrfToken=<token>&username=clerk1&password=Clerk@123" \
  http://localhost:8080/tax-business-system/auth/login.action
```

保存草稿（动态字段以 `field_` 前缀）：

```bash
curl -b cookies.txt -X POST \
  -d "csrfToken=<token>&taxpayerId=1&declarationTypeId=1" \
  -d "periodStart=2026-09-01&periodEnd=2026-09-30" \
  -d "field_sales_amount=100000.00&field_deductible_amount=20000.00" \
  -d "field_tax_method=一般计税&field_declaration_cycle=月度" \
  http://localhost:8080/tax-business-system/declarations/save.action
```

> 示例中的账号仅用于本地演示；生产部署必须更换密码并启用 HTTPS。
