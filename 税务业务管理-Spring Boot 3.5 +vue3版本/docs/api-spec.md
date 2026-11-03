# API 规范（V1.0）

> 适用版本：后端 Spring Boot 3.5.16，接口前缀 `/api/v1`。
> 本系统为课程教学模拟，所有数据均为虚构，不连接真实税务机关。

## 1. 通用约定

### 1.1 认证方式

- 会话认证（Spring Security `HttpSession`），登录成功后由服务端下发 `JSESSIONID` Cookie（`HttpOnly`、`SameSite=Lax`）。
- 浏览器端 Axios 需携带 `withCredentials: true`。
- 受保护接口在未登录时返回 `401`，已登录但缺少权限时返回 `403`。
- 登录、登出、当前用户、修改密码位于 `/api/v1/auth/*`。
- `POST /api/v1/auth/register` 允许匿名访问；新用户固定分配 `VIEWER` 角色。

### 1.2 统一响应结构

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "8f3a6c1d2e5b4a70"
}
```

- `code`：业务状态码，成功为 `OK`。
- `message`：提示信息；校验失败时为字段级说明。
- `data`：业务数据，无返回值时为 `null`（响应中省略）。
- `traceId`：服务端链路号，与操作日志 `trace_id` 对应。

### 1.3 分页结构

```json
{
  "items": [],
  "page": 1,
  "pageSize": 10,
  "total": 0
}
```

- 页码从 `1` 开始，`pageSize` 上限 100（服务端强制截断）。
- 列表排序字段采用白名单，非法值回退默认排序。

### 1.4 错误码

| HTTP | code | 场景 |
| ---- | ---- | ---- |
| 400 | `BAD_REQUEST` / `VALIDATION_ERROR` | 参数格式错误、Jakarta Validation 失败 |
| 401 | `UNAUTHORIZED` | 未登录或会话失效 |
| 403 | `FORBIDDEN` | 已登录但无权限 |
| 404 | `NOT_FOUND` | 资源不存在 |
| 409 | `CONFLICT` | 唯一约束、状态冲突、重复提交/审核 |
| 500 | `INTERNAL_ERROR` | 未预期异常（不返回堆栈） |

### 1.5 数据格式

- 日期：`YYYY-MM-DD`；日期时间：`YYYY-MM-DDTHH:mm:ss`（`Asia/Shanghai`）。
- 金额：JSON 数字，数据库 `DECIMAL(18,2)`。
- 枚举：字符串常量，状态同时包含文本与颜色展示。
- 所有写接口使用 Jakarta Validation；Controller 不承载业务校验。

## 2. 权限清单

权限编码与角色初始绑定见 `V2__seed_demo_data.sql`。后端以 `@PreAuthorize("hasAuthority('...')")` 为安全边界，前端菜单/按钮隐藏仅为体验优化。

| 模块 | 菜单权限 | 读权限 | 写权限 | 其他 |
| ---- | -------- | ------ | ------ | ---- |
| 工作台 | `dashboard:view` | - | - | - |
| 用户 | `user:menu` | `user:read` | `user:write` | - |
| 角色 | `role:menu` | `role:read` | `role:write` | - |
| 纳税人 | `taxpayer:menu` | `taxpayer:read` | `taxpayer:write` | - |
| 申报类型 | `declaration-type:menu` | `declaration-type:read` | `declaration-type:write` | - |
| 表单模板 | `form-template:menu` | `form-template:read` | `form-template:write` | - |
| 申报 | `declaration:menu` | `declaration:read` | `declaration:write` | `declaration:submit`、`declaration:withdraw`、`declaration:export` |
| 审核 | `review:menu` | `review:read` | `review:write` | - |
| 综合查询 | `search:menu` | `search:read` | - | 导出需 `declaration:export` |
| 统计 | `statistics:menu` | `statistics:read` | - | - |
| 日志 | `audit:menu` | `audit:read` | - | - |

## 3. 认证接口

### POST `/api/v1/auth/register`

- 权限：公开。
- 请求字段：`username`、`displayName`、`password`、`confirmPassword`。
- 账号长度 3-64，只允许字母、数字、下划线、点和短横线。
- 密码至少 8 位，必须同时包含字母和数字。
- 新账号固定分配 `VIEWER` 只读角色，不接受客户端传入角色信息。
- 成功返回 `200`；重复账号返回 `409`；非法格式、弱密码或确认密码不一致返回 `400`。

### POST `/api/v1/auth/login`

- 权限：公开
- 请求：

```json
{ "username": "clerk", "password": "Demo@1234" }
```

- 成功 `200`：返回 `CurrentUserVO`（`id`、`username`、`displayName`、`roles`、`permissions`、`menus`）。
- 失败 `401`：账号或密码错误、账号停用均返回通用认证失败信息，不区分具体原因。
- 登录成功更新 `last_login_at` 并写入 `LOGIN` 操作日志（不记录密码）。

### POST `/api/v1/auth/logout`

- 权限：已登录
- 注销当前会话，返回 `{"code":"OK"}`。

### GET `/api/v1/auth/me`

- 权限：已登录
- 返回当前用户、角色、权限编码、可见菜单（按 `sort_order` 排序）。

### PUT `/api/v1/auth/password`

- 权限：已登录
- 请求：

```json
{ "oldPassword": "Demo@1234", "newPassword": "NewPass2026" }
```

- 新密码需 8–64 位且同时包含字母和数字，且不能与当前密码相同；旧密码错误或新旧相同返回 `400`；成功写入 `PASSWORD_CHANGE` 操作日志。

## 4. 用户、角色与权限

### 用户

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/users?page&pageSize&keyword&status&roleId` | `user:read` | 分页查询，`keyword` 匹配账号/显示名 |
| GET | `/users/{id}` | `user:read` | 用户详情（含角色） |
| POST | `/users` | `user:write` | 新建用户 |
| PUT | `/users/{id}` | `user:write` | 更新显示名、状态、备注、角色 |
| PATCH | `/users/{id}/status` | `user:write` | `{"status":"ACTIVE"}` / `INACTIVE` |
| POST | `/users/{id}/reset-password` | `user:write` | `{"newPassword":"..."}` |
| DELETE | `/users/{id}` | `user:write` | 逻辑删除；不能删除自己 |

新建请求：

```json
{
  "username": "auditor",
  "password": "Audit2026",
  "displayName": "稽核员·样例",
  "status": "ACTIVE",
  "remark": "演示账号",
  "roleIds": [5]
}
```

约束：账号 3–64 位、字母开头、仅字母/数字/下划线；密码 8–64 位且含字母与数字；至少一个角色；密码 BCrypt(10) 存储，接口与日志均不回显。

### 角色与权限

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/roles?page&pageSize&keyword&status` | `role:read` | 角色分页（含用户数、权限数） |
| POST | `/roles` | `role:write` | 新建角色 |
| PUT | `/roles/{id}` | `role:write` | 更新角色名称、描述、状态 |
| PUT | `/roles/{id}/permissions` | `role:write` | `{"permissionIds":[1,7,22]}` 覆盖式分配 |
| GET | `/roles/permissions` | `role:read` | 全部权限（`MENU` / `API` 分组由前端完成） |

## 5. 纳税人

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/taxpayers?page&pageSize&keyword&type&status` | `taxpayer:read` | 分页；`type` 为 `ENTERPRISE/INDIVIDUAL/OTHER` |
| GET | `/taxpayers/options` | `taxpayer:read` | 启用中的纳税人下拉项 |
| GET | `/taxpayers/{id}` | `taxpayer:read` | 详情 |
| GET | `/taxpayers/{id}/declarations?page&pageSize` | `declaration:read` | 关联申报历史 |
| POST | `/taxpayers` | `taxpayer:write` | 新建，编号唯一 |
| PUT | `/taxpayers/{id}` | `taxpayer:write` | 更新 |
| PATCH | `/taxpayers/{id}/status` | `taxpayer:write` | `{"registrationStatus":"INACTIVE"}` |
| DELETE | `/taxpayers/{id}` | `taxpayer:write` | 有申报历史时拒绝，改为停用 |

请求示例：

```json
{
  "taxpayerCode": "TP20260007",
  "taxpayerName": "示例虚构咨询有限公司",
  "taxpayerType": "ENTERPRISE",
  "contactName": "样例联系人",
  "contactPhone": "13800000007",
  "remark": "仅用于教学演示"
}
```

编号重复返回 `409 CONFLICT`。

## 6. 申报类型

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/declaration-types?page&pageSize&keyword&enabled` | `declaration-type:read` | 分页 |
| GET | `/declaration-types/options` | `declaration-type:read` | 下拉项（含默认模板） |
| GET | `/declaration-types/{id}` | `declaration-type:read` | 详情 |
| POST | `/declaration-types` | `declaration-type:write` | 新建 |
| PUT | `/declaration-types/{id}` | `declaration-type:write` | 更新 |
| PATCH | `/declaration-types/{id}/status` | `declaration-type:write` | `{"enabled":false}` |

类型编码规则：大写字母开头，3–64 位大写字母、数字或下划线。停用类型不得用于新建申报；已存在的申报不受影响。

## 7. 表单模板

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/form-templates?page&pageSize&keyword&declarationTypeId&status` | `form-template:read` | 分页 |
| GET | `/form-templates/{id}` | `form-template:read` | 模板及字段 |
| GET | `/form-templates/{id}/preview` | `form-template:read` | 预览 |
| GET | `/form-templates/{id}/versions` | `form-template:read` | 同编码版本历史 |
| POST | `/form-templates` | `form-template:write` | 新建草稿（可带字段） |
| PUT | `/form-templates/{id}` | `form-template:write` | 仅草稿可修改 |
| POST | `/form-templates/{id}/fields` | `form-template:write` | 添加字段 |
| PUT | `/form-templates/{id}/fields/{fieldId}` | `form-template:write` | 修改字段 |
| DELETE | `/form-templates/{id}/fields/{fieldId}` | `form-template:write` | 删除字段 |
| POST | `/form-templates/{id}/publish` | `form-template:write` | 发布；至少一个启用字段 |
| POST | `/form-templates/{id}/disable` | `form-template:write` | 停用 |
| POST | `/form-templates/{id}/new-version` | `form-template:write` | 基于当前版本生成新草稿 |

字段请求：

```json
{
  "fieldCode": "income_amount",
  "fieldLabel": "模拟收入金额（元）",
  "fieldType": "DECIMAL",
  "required": true,
  "minValue": 0,
  "maxValue": 999999999.99,
  "maxLength": null,
  "options": [],
  "helpText": "仅用于教学统计",
  "sortOrder": 1,
  "enabled": true
}
```

- 字段类型：`TEXT`、`INTEGER`、`DECIMAL`、`DATE`、`RADIO`、`MULTI_SELECT`、`BOOLEAN`。
- 发布后版本不可原位修改，只能创建新版本；申报绑定提交时的模板版本。
- 模板配置不执行任意 JavaScript、SQL 或表达式。

## 8. 申报

状态机：

```
DRAFT ──submit──> PENDING_REVIEW ──approve──> APPROVED
  ▲                    │  │
  │                    │  └──withdraw──> WITHDRAWN
  └──────edit──────── RETURNED <──return──┘
```

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/declarations?page&pageSize&keyword&declarationNo&declarationTypeId&status` | `declaration:read` | 分页；经办人仅见本人数据 |
| GET | `/declarations/{id}` | `declaration:read` | 详情（含字段定义、审核历史） |
| GET | `/declarations/{id}/reviews` | `declaration:read` | 审核记录 |
| POST | `/declarations` | `declaration:write` | 新建草稿 |
| PUT | `/declarations/{id}` | `declaration:write` | 仅 `DRAFT` / `RETURNED` 可编辑 |
| POST | `/declarations/{id}/submit` | `declaration:submit` | 校验后进入待审核 |
| POST | `/declarations/{id}/withdraw` | `declaration:withdraw` | 仅 `PENDING_REVIEW` 可撤回 |

创建/更新请求：

```json
{
  "taxpayerId": 3,
  "declarationTypeId": 1,
  "periodStart": "2027-01-01",
  "periodEnd": "2027-01-31",
  "declaredAmount": 12000.00,
  "formData": {
    "period_note": "模拟月度申报",
    "income_amount": 12000,
    "employee_count": 5,
    "has_branch": false,
    "business_category": "SERVICE"
  }
}
```

服务端校验：

- 纳税人、申报类型存在且启用；申报期间起止合法。
- 模板必须为 `PUBLISHED`；按模板字段校验必填、类型、长度、数值范围、选项值。
- 同一纳税人 + 类型 + 期间，在 `DRAFT/PENDING_REVIEW/APPROVED` 状态只允许一条有效申报（`409`）。
- 编号 `DEC-yyyyMMdd-######` 由后端生成（6 位随机数，最多重试 5 次），数据库唯一约束兜底。
- 状态变更使用条件 UPDATE（带原状态），影响行数为 0 时返回 `409`，避免并发重复提交。
- 已提交不可由经办人修改；已通过不可编辑。

## 9. 审核

| 方法 | 路径 | 权限 | 说明 |
| ---- | ---- | ---- | ---- |
| GET | `/reviews/pending?page&pageSize&keyword&typeId` | `review:read` | 待审核列表 |
| GET | `/reviews/history?page&pageSize&keyword&action&reviewerId&from&to` | `review:read` | 审核历史 |
| POST | `/declarations/{id}/review/approve` | `review:write` | 通过 |
| POST | `/declarations/{id}/review/return` | `review:write` | 退回，原因必填 |

审核请求：

```json
{ "comment": "材料完整，模拟审核通过。" }
```

- 仅 `PENDING_REVIEW` 可审核；重复/并发审核返回 `409`。
- 退回时 `comment` 去除空白后不能为空，否则 `400`。
- 经办人不能审核自己的申报（`403`）。
- 状态更新与 `declaration_review` 记录在同一事务写入。

## 10. 统计

所有接口需 `statistics:read`，筛选参数一致：`from`、`to`（`YYYY-MM-DD`）、`typeId`；趋势额外支持 `months`（默认 6，1–24）。

| 方法 | 路径 | 返回 |
| ---- | ---- | ---- |
| GET | `/statistics/overview` | 总量、草稿、待审、通过、退回、撤回、金额合计、通过率 |
| GET | `/statistics/declarations/trend?months=6` | `[{month,count,amount}]` |
| GET | `/statistics/declarations/by-status` | `[{status,count}]` |
| GET | `/statistics/declarations/by-type` | `[{typeName,count,amount}]` |
| GET | `/statistics/declarations/by-taxpayer-type` | `[{taxpayerType,count,amount}]` |
| GET | `/statistics/dashboard` | 上述聚合 + `todos`（待办）+ `recentLogs` |

统计口径：按申报期间（`period_start/period_end`）归属月份；通过率 = `APPROVED / (APPROVED + RETURNED)`，分母为 0 时返回 `null`；全部为服务端 SQL 聚合，无硬编码数值。

## 11. 综合查询与导出

### GET `/api/v1/search/declarations`

- 权限：`search:read`
- 参数：`keyword`、`declarationNo`、`taxpayerCode`、`taxpayerName`、`declarationTypeId`、`status`、`periodFrom`、`periodTo`、`submittedFrom`、`submittedTo`、`minAmount`、`maxAmount`、`createdBy`、`reviewerId`、`sortBy`、`sortOrder`、`page`、`pageSize`。
- 排序白名单：`submittedAt`、`declaredAmount`、`declarationNo`、`createdAt`、`updatedAt`；方向 `asc/desc`。
- 经办人（CLERK）自动附加本人数据范围，不能通过参数越权。

### GET `/api/v1/search/declarations/export`

- 权限：`declaration:export`
- 与查询相同的筛选参数；最多导出 5000 行，超出时返回 `400`，提示缩小筛选范围。
- 返回 `text/csv;charset=UTF-8`，文件名 `declarations-yyyyMMddHHmmss.csv`，内容含 UTF-8 BOM。
- 以 `= + - @` 开头的单元格加前缀 `'`，防止公式注入。
- 每次导出写入 `DECLARATION_EXPORT` 操作日志（记录筛选条件摘要与行数，不记录敏感值）。

## 12. 操作日志

### GET `/api/v1/audit-logs`

- 权限：`audit:read`
- 参数：`page`、`pageSize`、`operatorId`、`operationType`、`result`、`from`、`to`、`keyword`。
- 返回字段：`operatorId`、`operatorName`、`operationType`、`targetType`、`targetId`、`result`、`summary`、`traceId`、`createdAt`。
- 日志只读，无修改/删除接口；不记录密码、Token、完整请求体。

## 13. 健康检查

- `GET /actuator/health`：无需认证，返回数据库与磁盘状态，不暴露细节。
- `GET /v3/api-docs`、`/swagger-ui.html`：开发环境可用，生产可通过 `springdoc.*.enabled=false` 关闭。
