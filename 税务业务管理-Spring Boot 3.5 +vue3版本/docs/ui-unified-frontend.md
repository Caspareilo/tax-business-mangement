# Vue 3 前端与传统 Java EE 版视觉统一记录

- 日期：2026-11-01
- Vue 项目：Spring Boot 3.5 + Vue 3 + Element Plus
- 对照项目：传统 Java EE + Servlet/JSP

## 1. 边界

两个项目继续保持独立技术栈和独立目录：

- Vue 项目继续使用 Vue 3、TypeScript、Public API、Pinia、Vue Router、Element Plus 和 ECharts。
- 传统项目继续使用 Servlet、JSP、Bootstrap 和 Chart.js。
- 本次只统一前端设计语言、布局、登录体验和页面视觉，没有迁移或合并框架。
- 没有修改 Spring Boot 接口、数据库、权限规则或业务状态机。

## 2. 统一内容

### 全局视觉

- 页面背景：`#F0F2F5`
- 内容面板：`#FFFFFF`
- 主色：`#1890FF`
- 主色悬停：`#096DD9`
- 主要文字：`#262626`
- 次级文字：`#595959`
- 辅助文字：`#8C8C8C`
- 边框：`#F0F0F0`
- 分割线：`#E8E8E8`
- 成功、警告、错误色与 JSP 版保持一致。

`frontend/src/styles/main.css` 统一覆盖 Element Plus 主题、按钮、输入框、表格、分页、弹窗、抽屉、状态标签、筛选区、详情列表和卡片样式。

### 公共布局

- 侧栏调整为 232px 白色导航，右侧细分割线。
- 顶部栏高度调整为 56px，左侧显示系统名与当前页面，右侧显示中文角色和当前用户。
- 导航按“总览 / 业务办理 / 基础数据 / 系统管理”分组，权限仍来自后端菜单列表。
- 桌面端支持侧栏收起；窄屏切换为遮罩式抽屉，并在导航后自动关闭。

### 登录页

- 使用与传统版一致的左侧浅灰业务插画区、右侧白色登录区和紫色登录交互。
- 五个测试账号收纳在登录按钮下方的一行紧凑标签，点击后直接使用原有 `/auth/login` 接口登录。
- 保留“角色说明”页签、密码显示/隐藏、账号与密码校验。
- 左下角显示 `Designed by 吴学孟 · 仅限课程演示使用`。

### Dashboard

- 四项指标卡：申报总量、待审核、审核通过、模拟金额合计。
- 数量与金额趋势可切换；趋势数据来自后端聚合。
- 类型金额排名来自真实 `byType` 数据。
- 待办申报、状态环图和业务队列均来自 Dashboard 接口。
- 最近操作继续使用真实审计日志数据。

## 3. 验收结果

### 工程检查

- `npm run build`：通过。
- `npm run lint`：通过。
- 后端仍为独立 Spring Boot 工程，接口路径未改动。

### 浏览器检查

| 检查项 | 结果 |
| --- | --- |
| Vue 登录页 `1440 × 900` | 主卡片 `1160 × 760`，无横向溢出，控制台无错误 |
| Vue 登录页 `1366 × 768` | 主卡片高度 750px，无纵向额外滚动，无横向溢出 |
| Vue 登录页 `390 × 844` | 三列紧凑测试账号，无横向溢出 |
| Dashboard `1440 × 900` | 四张等宽指标卡、趋势切换、类型排名、待办和状态环图正常 |
| Dashboard `1366 × 768` | 四列指标，无横向溢出 |
| Dashboard `768 × 900` | 两列指标，侧栏隐藏 |
| Dashboard `390 × 844` | 单列指标，侧栏抽屉可打开并在导航后关闭 |
| 快捷登录 | `admin / clerk / reviewer / analyst / viewer` 五个账号均成功进入对应角色 |
| 菜单权限 | 五个角色菜单数分别为 11、7、8、6、5，符合后端权限 |
| 申报表单 | Element Plus 弹窗正常打开，无页面横向溢出 |

## 4. 截图

截图位于 `docs/screenshots/`：

- `ui-unified-login-1440x900.png`
- `ui-unified-login-register-1440x900.png`
- `ui-unified-login-1366x768.png`
- `ui-unified-login-390x844.png`
- `ui-unified-dashboard-1440x900.png`
- `ui-unified-dashboard-1366x768.png`
- `ui-unified-dashboard-768x900.png`
- `ui-unified-dashboard-390x844.png`
- `ui-unified-declarations-1440x900.png`
- `ui-unified-declaration-form-1440x900.png`
- `ui-unified-statistics-1440x900.png`
