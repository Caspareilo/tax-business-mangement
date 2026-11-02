# 税务业务管理系统开发文档 V1.0

> 文档状态：需求与技术基线草案\
> 版本：V1.0\
> 项目类型：Java EE / Java Web 课程设计\
> 目标：构建可本地运行、可演示、可测试、可继续迭代的教学用税务业务管理系统\
> 重要声明：本系统仅用于课程教学和软件工程实践。使用虚构纳税人和模拟业务规则，不连接真实税务机关，不提交真实纳税申报，不提供法律或税务专业结论。

## 0. 文档使用说明

本文档作为 V1.0
项目基线。若课程老师另有明确要求，以课程要求为最高优先级，并在变更记录中记录差异。任何新增功能应先更新需求、数据模型、接口和验收用例，再实施代码变更。

需求优先级： - **P0（必须）**：课程项目核心闭环及可验收的基础能力。 -
**P1（重要）**：提高完整性、可用性和演示效果。 -
**P2（可选）**：在时间充足时实现，不得影响 P0 稳定性。

V1.0
不追求真实税务系统的复杂度，不引入微服务、消息队列、分布式事务、独立数据仓库等不必要组件。

------------------------------------------------------------------------

# 1. 项目概述

## 1.1 项目背景

项目原始提示为"税务业务管理：权限 / 申报 / 统计 / 表单 / 查询"。V1.0
将其扩展为面向教学场景的业务管理系统，围绕用户权限、纳税人资料、申报表单、申报提交、审核处理、统计分析和综合查询形成完整业务闭环。

## 1.2 项目目标

1.  实现用户登录、角色分配和基于权限的菜单及操作控制。
2.  管理模拟纳税人资料和申报类型。
3.  支持申报表单填写、字段校验、草稿保存和正式提交。
4.  支持审核人员查看待审核任务、通过或退回申报，并保存审核意见。
5.  支持多条件查询、分页、排序和授权范围内的数据导出。
6.  通过图表展示申报数量、状态分布、模拟税额和趋势。
7.  提供表单模板与字段配置的基础管理能力。
8.  提供操作审计日志、统一异常处理和基本安全防护。
9.  提供初始化数据、启动说明、接口说明、测试用例和验收标准。
10. 保证代码结构清晰，便于后续 V1.1、V1.2 迭代。

## 1.3 项目范围

### 范围内

-   教学用账号登录与退出。
-   用户、角色、菜单/操作权限管理。
-   模拟纳税人资料管理。
-   申报类型与申报期限的基础配置。
-   表单模板、字段定义和基础校验配置。
-   申报草稿、提交、审核通过、退回修改、合规撤回。
-   申报历史、审核记录、操作日志。
-   首页工作台、统计图表和综合查询。
-   CSV 导出或 XLSX 导出（二选一；推荐先做 CSV）。
-   本地开发环境和单机部署。

### 范围外

-   连接真实税务机关或真实税务接口。
-   真实纳税申报、真实缴税、银行支付。
-   真实身份证件、真实企业税务资料或其他敏感生产数据。
-   电子签章、CA 认证、真实发票开具、税务法规自动判定。
-   多租户 SaaS、微服务、集群部署、复杂审批工作流引擎。
-   无人工审核的真实税务结论。

## 1.4 成功标准

-   新环境按文档能完成数据库初始化、前后端启动和登录。
-   核心业务可从"登录---选择纳税人---填表---提交---审核---统计查询"完整演示。
-   未授权用户无法仅通过直接调用 API 执行无权操作。
-   后端校验状态转换、必填字段和金额规则，不依赖前端单独保证。
-   列表查询支持分页与组合筛选，不将全部记录一次性返回。
-   所有核心 P0 功能有可重复执行的测试用例。
-   页面无明显错位、死链、空按钮或仅展示不工作的假交互。
-   所有演示数据均为虚构数据。

------------------------------------------------------------------------

# 2. 用户角色与权限模型

## 2.1 角色定义

### 系统管理员 `ADMIN`

-   管理用户、角色、权限和字典。
-   管理申报类型、表单模板及系统参数。
-   查看系统操作日志。
-   具备管理权限，但不得绕过业务状态规则直接篡改审核历史。

### 业务经办人 `CLERK`

-   查看被授权的纳税人资料。
-   创建、编辑、保存本人有权处理的申报草稿。
-   提交申报。
-   查看授权范围内的申报记录。
-   按规则撤回尚未进入不可撤回阶段的申报。

### 审核人员 `REVIEWER`

-   查看待审核申报。
-   查看申报内容与相关历史。
-   执行审核通过或退回修改。
-   填写审核意见。
-   查看本人处理过的审核记录。
-   不得审核自己提交的申报（V1.0 建议采用此规则）。

### 统计查看人员 `ANALYST`

-   查看授权范围内的统计看板。
-   按条件查询汇总数据。
-   导出获准导出的统计结果。
-   默认无申报编辑和审核权限。

### 演示只读用户 `VIEWER`（可选）

-   仅查看演示数据和允许公开的页面。
-   不可新增、修改、删除或审核业务记录。

## 2.2 权限原则

1.  **前端权限只负责体验，后端权限才是安全边界。**
2.  未登录访问受保护 API 返回 HTTP 401。
3.  已登录但没有相应权限返回 HTTP 403。
4.  用户被停用后不得继续登录；已有会话应在合理时间内失效。
5.  默认拒绝未明确授权的操作。
6.  菜单权限、按钮权限、API 权限和数据范围是不同层次，不能只隐藏菜单。
7.  所有高影响操作必须校验当前业务状态。
8.  角色分配、权限变更、用户停用和审核操作均记录审计日志。

## 2.3 权限矩阵（初版）

  功能             ADMIN                CLERK            REVIEWER       ANALYST      VIEWER
  ---------------- -------------------- ---------------- -------------- ------------ --------------
  登录、个人资料   是                   是               是             是           是
  用户管理         是                   否               否             否           否
  角色与权限管理   是                   否               否             否           否
  纳税人查询       是                   授权范围         授权范围       授权范围     只读授权范围
  纳税人编辑       是                   按业务授权       否             否           否
  新建/编辑草稿    可管理但受规则约束   是               否             否           否
  提交申报         可配置               是               否             否           否
  审核申报         不默认代审           否               是             否           否
  统计看板         是                   是（必要范围）   是             是           只读
  表单模板管理     是                   否               否             否           否
  操作日志         是                   否               限定审核记录   否           否
  数据导出         是                   按授权范围       按授权范围     按授权范围   默认否

数据范围 V1.0
可简化为"全量范围"与"指定纳税人范围"两种；若课程时间不足，至少必须实现角色/操作权限和服务端授权校验。不得假装已经实现复杂组织级数据隔离。

------------------------------------------------------------------------

# 3. 功能需求详细说明

## 3.1 首页与工作台

### 页面内容

-   顶部显示系统名称、当前用户、角色、个人菜单和退出入口。
-   左侧导航按权限动态显示。
-   面包屑和页面标题。
-   指标卡：申报总数、待审核数、已通过数、已退回数。
-   图表：近 6 或 12
    个月申报数量趋势；申报状态分布；按申报类型的模拟税额统计。
-   待办区域：当前审核人员的待审核任务，或经办人的草稿/退回待修改事项。
-   最近操作记录（仅显示当前用户有权查看的内容）。
-   常用入口：新建申报、申报查询、待审核列表、统计分析。

### 规则

-   指标必须由后端真实查询结果生成，不能写死演示数字。
-   所有卡片和图表应遵守当前用户的数据权限。
-   空数据时显示明确的空状态，不显示虚构趋势。
-   点击指标可跳转至带相应筛选条件的列表页。

## 3.2 用户与权限管理

### 用户列表

字段：用户编号、登录账号、显示名称、角色、状态、最近登录时间、创建时间、操作。
支持：按账号/名称模糊查询、角色筛选、状态筛选、分页、排序。

### 新增/编辑用户

字段：登录账号、显示名称、初始密码（新建时）、角色、状态、备注（可选）。
校验： - 账号必填、唯一，长度限制合理。 -
密码满足课程项目最低复杂度要求。 -
不允许将密码明文返回给前端或写入日志。 - 用户停用后不能登录。 -
删除用户采用软删除或停用；若有关联业务记录，禁止物理删除。

### 角色与权限

-   角色名称、角色编码、描述、状态。
-   为角色分配菜单和操作权限。
-   角色编码唯一。
-   禁止删除系统内置且仍被使用的角色；可通过停用处理。
-   权限更新后应重新加载或刷新会话权限。
-   后端对每个受保护接口声明权限要求。

### 认证与会话

-   采用 Spring Security。
-   V1.0 优先采用服务端 Session 或短期 JWT
    二选一，不能混用两套未经设计的认证方式。课程本地部署推荐
    Session；若使用 JWT，需明确过期、刷新、退出和存储策略。
-   密码使用 BCrypt 等专用密码哈希算法保存。
-   登录失败返回通用提示，避免暴露账号是否存在。
-   对登录失败做基础频率限制或锁定策略（可作为 P1）。
-   登出应使当前会话失效。

## 3.3 纳税人信息管理

### 列表字段

模拟纳税人编号、名称、主体类型、联系人（可选）、联系电话（虚构）、登记状态、创建时间、操作。

### 字段建议

-   `taxpayer_code`：唯一业务编号。
-   `taxpayer_name`：名称。
-   `taxpayer_type`：个人/企业/其他模拟类型。
-   `contact_name`：联系人（可选，使用虚构数据）。
-   `contact_phone`：虚构联系电话，可选。
-   `registration_status`：ACTIVE/INACTIVE。
-   `remark`：备注。
-   `created_at`、`updated_at`。

### 功能

-   列表、详情、新增、编辑、停用/启用。
-   查询条件：编号、名称、类型、状态。
-   展示该纳税人的申报历史。
-   存在申报记录时不物理删除纳税人，只能停用。
-   不收集真实身份证号、真实税号或其他真实敏感资料。

## 3.4 申报类型管理

初始模拟类型可配置为"模拟综合申报""模拟经营申报""模拟个人申报"，名称只用于教学，不代表真实税法分类。

字段： -
申报类型编码、名称、说明、表单模板版本、是否启用、默认期限规则（模拟）、创建/更新时间。

规则： - 停用的类型不可用于新建申报。 -
已有申报保留创建时的类型名称/编码快照，避免类型重命名影响历史记录。 -
税额计算仅采用文档明确的模拟公式或用户填写的模拟金额，不宣称符合现行税法。

## 3.5 表单模板管理

### 模板管理

字段：模板编码、模板名称、关联申报类型、版本号、状态（草稿/已发布/已停用）、创建人、发布时间。
功能：新增模板、编辑草稿、配置字段、预览、发布、停用、查看版本。

### 字段配置

字段定义至少包括： - 字段编码（稳定且唯一）。 - 字段标签。 -
字段类型：文本、整数、小数/金额、日期、单选、多选、布尔。 - 是否必填。 -
最大长度/数值范围。 - 选项配置（适用于单选/多选）。 - 排序号。 -
帮助说明。 - 基础校验规则。 - 是否启用。 - 是否属于敏感展示字段（V1.0
原则上不配置真实敏感字段）。

### 版本规则

-   只有草稿模板可编辑。
-   发布后生成不可变版本。
-   修改已发布模板时创建新版本，不直接覆盖旧版本。
-   申报创建时绑定模板版本 ID。
-   已提交申报始终按照其绑定的模板版本展示。
-   发布前校验字段编码唯一、选项合法、规则可解析。
-   不允许执行模板配置中传入的任意 JavaScript、SQL 或表达式代码。
-   动态字段值可采用 JSON 存储，但模板元数据仍应规范化保存。

## 3.6 纳税申报管理

### 列表字段

申报编号、纳税人、申报类型、申报期间、模拟申报金额/税额、状态、提交时间、创建人、操作。

### 新建申报

1.  选择纳税人。
2.  选择已启用的申报类型。
3.  系统绑定当前已发布模板版本。
4.  根据字段定义渲染表单。
5.  前端提供即时校验；后端再次执行权威校验。
6.  用户可保存草稿。
7.  用户确认后提交。

### 申报数据

推荐主表保存稳定的查询字段，动态表单数据保存为 JSON： -
主表：申报编号、纳税人 ID、申报类型 ID、模板版本
ID、申报期间、状态、模拟金额、创建人、提交时间等。 - JSON
载荷：动态字段答案。 -
重要的金额/状态/时间等用于查询和统计的字段不能只藏在 JSON 内。

### 业务规则

-   申报编号由后端生成，唯一。
-   草稿状态可编辑。
-   待审核状态不可直接编辑。
-   已退回状态可由原经办人或授权人员修改后重新提交，并保留之前的审核历史。
-   审核通过后默认不可编辑；若要修改，应通过新申报或明确的更正流程，不允许直接覆盖历史。
-   同一纳税人、申报类型、申报期间的重复申报规则必须明确。V1.0
    推荐默认禁止存在另一条"草稿/待审核/审核通过"的同类型同期间有效申报；若业务需要更正，后续版本再定义更正关系。
-   提交操作需防止重复点击导致重复记录；可通过业务唯一约束、状态校验和事务处理实现。
-   后端重新校验模板版本、字段必填项、类型、数值范围及申报期间。
-   金额采用 `DECIMAL(18,2)` 或类似精度；不使用 `float/double`
    存储金额。

## 3.7 申报审核管理

### 待审核列表

字段：申报编号、纳税人名称、申报类型、申报期间、提交时间、提交人、等待时长、操作。

### 审核详情

显示： - 申报基础信息。 - 按提交时模板版本渲染的表单内容。 -
模拟金额及关键字段。 - 历史审核意见。 - 提交人和提交时间。 -
通过/退回按钮及审核意见输入框。

### 审核规则

-   只有 REVIEWER 或明确获授权人员可审核。
-   只能审核 `PENDING_REVIEW` 状态的申报。
-   审核通过后状态变为 `APPROVED`。
-   退回修改后状态变为 `RETURNED`，必须填写退回原因。
-   同一审核请求只能成功执行一次。
-   V1.0 禁止经办人审核自己提交的申报。
-   每次审核都新增 `declaration_review` 记录，不覆盖旧记录。
-   审核更新申报状态与新增审核记录必须在同一事务中完成。
-   并发情况下采用状态条件更新或乐观锁，避免两名审核人员同时成功处理同一条申报。

## 3.8 统计分析

### 统计维度

-   时间：日、月（V1.0 至少支持月份范围）。
-   申报类型。
-   申报状态。
-   纳税人主体类型。
-   审核结果。

### 指标定义

-   申报总量：符合筛选范围的申报记录数。
-   待审核量：状态为 `PENDING_REVIEW` 的记录数。
-   通过量：状态为 `APPROVED` 的记录数。
-   退回量：状态为 `RETURNED` 的记录数。
-   模拟申报金额合计：仅统计符合授权和筛选范围的记录，并在页面标明"教学模拟金额"。
-   审核通过率：建议定义为已完成审核记录中的通过数 /（通过数 +
    退回数），分母为 0 时显示"---"。
-   月度趋势：按提交月份或创建月份统计，必须在图表标题中明确口径。

### 规则

-   统计接口在服务端执行聚合查询。
-   统计结果遵守数据权限。
-   日期区间采用明确的起止规则，建议开始时间包含、结束时间包含到当天结束或采用左闭右开区间。
-   图表提供加载中、空数据和错误状态。
-   导出数据必须与当前筛选条件一致，并遵守数据权限。

## 3.9 综合查询

支持查询： - 申报编号精确查询。 - 纳税人编号/名称查询。 - 申报类型。 -
状态。 - 申报期间。 - 提交时间范围。 - 经办人/审核人员（仅授权角色）。 -
金额范围（可选）。

要求： - 支持组合筛选、分页、排序和重置条件。 -
默认按更新时间或提交时间倒序。 -
后端白名单控制可排序字段，禁止把用户传入的任意字符串拼接为 SQL。 -
查询条件应可从详情页返回后保留（可选）。 -
导出操作有权限校验、条数限制和操作日志。 - 不允许通过修改 URL
参数越权查询其他数据。

## 3.10 系统设置与审计

-   字典管理：状态、申报类型类别等基础枚举；业务状态代码不可随意删除。
-   模拟申报期限配置：用于演示逾期提醒，不代表真实法定期限。
-   操作日志：登录、登出、用户管理、角色权限修改、申报提交、审核、导出等关键事件。
-   日志字段：操作者 ID、操作类型、业务对象类型、业务对象
    ID、结果、时间、请求追踪
    ID；避免记录密码、完整敏感载荷和不必要个人数据。
-   日志对普通用户只读且不可编辑；管理员可查询，不能通过普通 UI
    修改历史日志。
-   统一异常处理，向用户返回可理解提示，日志记录技术细节但不暴露给前端。

------------------------------------------------------------------------

# 4. 业务状态与流程约束

## 4.1 申报状态代码

  代码               中文       允许的后续动作
  ------------------ ---------- ----------------------------------------
  `DRAFT`            草稿       编辑、删除草稿（可选）、提交
  `PENDING_REVIEW`   待审核     审核通过、退回；符合规则时由经办人撤回
  `RETURNED`         已退回     编辑、重新提交
  `APPROVED`         审核通过   查看、统计；不可直接编辑
  `WITHDRAWN`        已撤回     查看、统计；终态

## 4.2 状态转换表

  ----------------------------------------------------------------------------------------
  当前状态          操作              目标状态          条件
  ----------------- ----------------- ----------------- ----------------------------------
  DRAFT             保存              DRAFT             创建人或授权经办人

  DRAFT             提交              PENDING_REVIEW    校验通过、无重复申报

  PENDING_REVIEW    审核通过          APPROVED          审核权限、非本人审核、状态未变化

  PENDING_REVIEW    退回              RETURNED          审核权限、退回意见必填

  PENDING_REVIEW    撤回              WITHDRAWN         仅允许的角色，且尚未被审核处理

  RETURNED          修改并保存        RETURNED          原经办人或授权人员

  RETURNED          重新提交          PENDING_REVIEW    重新校验，新增审核历史

  APPROVED          编辑              禁止              需未来版本定义更正业务

  WITHDRAWN         修改/提交         禁止              新建一条申报
  ----------------------------------------------------------------------------------------

所有状态转换在后端服务层集中实现，不允许 Controller
或前端直接随意赋值状态。

------------------------------------------------------------------------

# 5. 页面与 UI 设计规范

## 5.1 设计目标

界面应像成熟的企业管理后台，而不是营销网站或默认 AI
风格模板。强调信息清晰、阅读舒适、操作可预测、数据密度合理。

## 5.2 技术选择

-   Vue 3 + TypeScript + Vite。
-   Element Plus 作为基础组件库。
-   Vue Router 管理路由。
-   Pinia 管理用户、权限和跨页面状态。
-   Axios 或等价 HTTP 客户端封装 API。
-   ECharts 用于统计图表。
-   CSS 变量统一主题色、间距、圆角、阴影和边框。
-   图标使用统一的开源图标库，禁止混用大量风格不一致的图标。

## 5.3 视觉令牌（初始值，可经参考调研微调）

-   页面背景：`#F5F6F4` 或相近浅暖灰。
-   主内容卡片：`#FFFFFF`。
-   主文字：`#252B33`。
-   次级文字：`#68717D`。
-   边框：`#E2E5E8`。
-   主强调色：低饱和蓝灰 `#526B7A`。
-   辅助强调色：墨绿 `#567565`，仅用于少量成功/辅助信息。
-   警告：柔和琥珀色；危险：低饱和砖红。
-   禁止大面积紫色、荧光蓝、渐变背景、发光边框、过度阴影、无意义的玻璃拟态。
-   圆角以 4--8 px 为主，阴影轻微或不用。
-   页面主要通过间距、字号、边框和对齐建立层级，不依赖彩色卡片堆砌。

## 5.4 页面布局

-   左侧固定/可折叠导航栏，宽度建议 220--248 px。
-   顶部栏包含系统名称、面包屑/页面上下文、用户菜单。
-   内容区域有统一最大宽度与间距；数据密集页面允许使用全宽。
-   列表页：标题与主要操作 → 筛选区 → 表格 → 分页。
-   表单页：标题与返回入口 → 分组表单 → 底部操作区。
-   详情页：状态摘要 → 业务信息 → 表单数据 → 审核历史。
-   弹窗仅用于短操作；长表单使用独立页面。
-   页面应有加载、空数据、失败、无权限四类状态。
-   1440×900 和 1366×768
    为优先验收尺寸；小屏时侧栏可折叠，表格可横向滚动。

## 5.5 页面清单

1.  登录页。
2.  首页工作台。
3.  用户管理列表/新增编辑。
4.  角色管理列表/权限配置。
5.  纳税人列表/详情/新增编辑。
6.  申报类型列表/编辑。
7.  表单模板列表/字段编辑/预览/版本查看。
8.  新建申报/草稿编辑。
9.  我的申报列表/申报详情。
10. 待审核列表/审核详情。
11. 综合查询页。
12. 统计分析页。
13. 操作日志页。
14. 个人资料/修改密码。
15. 403 无权限、404 页面、通用错误页。

## 5.6 UI 参考调研流程

开发 Agent 必须在编码前进行网页调研： 1. 访问 Ant Design 官方设计规范与
Pro 页面，研究导航、筛选表单、数据表格和状态反馈。 2. 访问 Element Plus
官方文档，确认表格、表单、分页、日期范围、弹窗和反馈组件的正确用法。 3.
访问 Vue Vben Admin
官方文档或公开仓库，观察真实后台的信息架构和权限路由实现。 4. 至少记录 3
个可访问 URL、页面名称、参考点、采用/不采用的原因。 5.
优先引用官方文档、公开演示站和开源仓库；如果网页不可访问，不得编造访问结果，应改用可访问的官方文档或截图。
6.
不要求复制某一套模板。只借鉴成熟的信息结构与交互，保持本项目视觉统一。
7. 不下载或复用未经授权的商业素材，不复制品牌 Logo。 8. 输出
`docs/ui-reference-research.md`，包括调研日期、URL、观察点、截图路径（如有）和设计决策。

------------------------------------------------------------------------

# 6. 技术架构

## 6.1 选型

### 前端

-   Vue 3
-   TypeScript
-   Vite
-   Element Plus
-   Vue Router
-   Pinia
-   Axios
-   ECharts
-   ESLint + Prettier

### 后端

-   Java 17
-   Spring Boot 3.5.x 稳定维护线（项目创建时确认兼容版本；锁定具体 patch
    版本，不使用动态版本）
-   Spring Web
-   Spring Security
-   Jakarta Validation
-   MyBatis-Plus 的 Spring Boot 3 starter，或 MyBatis 原生
    Mapper（二选一；V1.0 推荐 MyBatis-Plus）
-   MySQL Connector/J
-   Flyway（推荐管理数据库迁移）
-   springdoc-openapi（生成 API 文档，可选但推荐）
-   JUnit 5、Spring Boot Test、MockMvc

### 数据库与工具

-   MySQL 8.0 系列。
-   Maven Wrapper（推荐）或 Maven。
-   Git。
-   Apifox/Postman 用于接口验证。
-   Docker Compose 可作为可选的本地数据库启动方式，不作为唯一运行方式。

版本原则：Spring Boot、MyBatis-Plus、JDK 和 MySQL
驱动必须确认兼容后锁定。Spring Boot 3.5 系列要求 Java 17 起步；具体
patch
版本以项目创建当天官方文档为准。不得将"最新版本"当作不加验证的选型依据。

## 6.2 架构风格

采用前后端分离的单体应用，逻辑分层如下：

`Vue 页面 → API 客户端 → Controller → Service → Mapper/Repository → MySQL`

-   Controller：HTTP 参数接收、参数格式校验、调用服务、统一响应。
-   Service：业务规则、状态转换、事务、权限相关业务校验。
-   Mapper/Repository：数据库访问，不放复杂业务规则。
-   Entity/DO：数据库映射对象。
-   DTO：请求/响应数据对象，避免直接暴露数据库实体。
-   VO：前端展示对象。
-   Security：身份认证、接口授权、会话策略。
-   Config：跨域、序列化、分页、OpenAPI 等配置。
-   Exception：业务异常、参数异常和统一异常处理。

不要将所有业务写在 Controller
中；不要让前端直接连接数据库；不要使用一张万能表保存所有业务。

## 6.3 推荐目录结构

``` text
tax-business-system/
├── README.md
├── docs/
│   ├── development-document-v1.0.md
│   ├── api-spec.md
│   ├── database-design.md
│   ├── test-plan.md
│   ├── ui-reference-research.md
│   └── changelog.md
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/
│       ├── assets/
│       ├── components/
│       ├── layouts/
│       ├── router/
│       ├── stores/
│       ├── styles/
│       ├── types/
│       ├── utils/
│       └── views/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/tax/
│       │   ├── common/
│       │   ├── config/
│       │   ├── security/
│       │   ├── auth/
│       │   ├── user/
│       │   ├── role/
│       │   ├── taxpayer/
│       │   ├── declarationtype/
│       │   ├── formtemplate/
│       │   ├── declaration/
│       │   ├── review/
│       │   ├── statistics/
│       │   └── audit/
│       └── test/java/
├── database/
│   ├── migration/
│   └── seed/
└── .env.example
```

包名 `com.example.tax` 应在实际开发时替换为课程项目约定的合法包名。

## 6.4 API 约定

-   API 前缀：`/api/v1`。
-   JSON 编码：UTF-8。
-   日期时间统一采用 ISO 8601 格式，明确时区策略。
-   列表接口采用分页。
-   成功响应格式统一，例如：

``` json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "request-trace-id"
}
```

-   分页数据建议格式：

``` json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

-   错误响应不返回堆栈、SQL、密钥或内部路径。
-   HTTP 状态码语义一致：400 参数错误，401 未认证，403 无权限，404
    资源不存在，409 状态冲突/重复业务，500 未预期服务器错误。
-   API 请求 DTO 使用 Jakarta Validation；Service
    再执行关键业务规则校验。
-   更新和审核接口应校验资源存在、权限、当前状态与必要的并发条件。

## 6.5 事务与并发

-   申报提交：业务校验、状态变更、提交时间更新、审计记录在同一事务中完成。
-   审核处理：状态条件更新与审核历史插入在同一事务中完成。
-   唯一业务规则由服务端校验并尽量通过数据库唯一索引兜底。
-   审核接口应防止重复提交。可使用条件更新，例如仅当状态仍为
    `PENDING_REVIEW` 时更新；受影响行数为 0 则返回 409。
-   不允许仅在前端禁用按钮来防止并发重复操作。

------------------------------------------------------------------------

# 7. 数据库设计

## 7.1 通用约定

-   表引擎：InnoDB。
-   字符集：`utf8mb4`。
-   主键：`BIGINT`，可使用自增 ID；若选择 UUID/雪花 ID，需全项目统一。
-   时间：`DATETIME` 或 `TIMESTAMP`，项目内统一时区策略。
-   金额：`DECIMAL(18,2)`。
-   状态：使用有约束的字符串/枚举代码，禁止在代码各处散落魔法字符串。
-   所有核心表包含创建时间；可变表包含更新时间。
-   外键策略需统一。课程项目可以使用数据库外键保证引用完整性；若团队决定不使用物理外键，必须通过服务层和测试保证关系一致。
-   业务记录不做无痕硬删除；优先使用停用或软删除。
-   敏感字段最小化，禁止保存真实密码、真实税号或不必要的真实个人数据。

## 7.2 表结构清单

### `sys_user`

-   `id BIGINT PK`
-   `username VARCHAR(64) NOT NULL UNIQUE`
-   `password_hash VARCHAR(255) NOT NULL`
-   `display_name VARCHAR(100) NOT NULL`
-   `status VARCHAR(20) NOT NULL`（ACTIVE/INACTIVE）
-   `last_login_at DATETIME NULL`
-   `created_at DATETIME NOT NULL`
-   `updated_at DATETIME NOT NULL`
-   `deleted TINYINT NOT NULL DEFAULT 0`

索引：唯一账号索引；状态索引（如常用于筛选）。

### `sys_role`

-   `id BIGINT PK`
-   `role_code VARCHAR(64) NOT NULL UNIQUE`
-   `role_name VARCHAR(100) NOT NULL`
-   `description VARCHAR(255) NULL`
-   `status VARCHAR(20) NOT NULL`
-   `created_at DATETIME NOT NULL`
-   `updated_at DATETIME NOT NULL`

### `sys_permission`

-   `id BIGINT PK`
-   `permission_code VARCHAR(100) NOT NULL UNIQUE`
-   `permission_name VARCHAR(100) NOT NULL`
-   `permission_type VARCHAR(20) NOT NULL`（MENU/BUTTON/API）
-   `parent_id BIGINT NULL`
-   `route_path VARCHAR(255) NULL`
-   `api_pattern VARCHAR(255) NULL`
-   `http_method VARCHAR(12) NULL`
-   `sort_order INT NOT NULL DEFAULT 0`
-   `status VARCHAR(20) NOT NULL`

### `sys_user_role`

-   `user_id BIGINT NOT NULL`
-   `role_id BIGINT NOT NULL`
-   联合主键/唯一约束：`(user_id, role_id)`
-   分别建立 `user_id`、`role_id` 索引。

### `sys_role_permission`

-   `role_id BIGINT NOT NULL`
-   `permission_id BIGINT NOT NULL`
-   联合主键/唯一约束：`(role_id, permission_id)`

### `taxpayer`

-   `id BIGINT PK`
-   `taxpayer_code VARCHAR(64) NOT NULL UNIQUE`
-   `taxpayer_name VARCHAR(200) NOT NULL`
-   `taxpayer_type VARCHAR(30) NOT NULL`
-   `contact_name VARCHAR(100) NULL`
-   `contact_phone VARCHAR(40) NULL`（仅虚构数据）
-   `registration_status VARCHAR(20) NOT NULL`
-   `remark VARCHAR(500) NULL`
-   `created_at DATETIME NOT NULL`
-   `updated_at DATETIME NOT NULL`
-   `deleted TINYINT NOT NULL DEFAULT 0`

索引：名称前缀/普通索引、类型与状态组合索引（按实际查询评估）。

### `declaration_type`

-   `id BIGINT PK`
-   `type_code VARCHAR(64) NOT NULL UNIQUE`
-   `type_name VARCHAR(100) NOT NULL`
-   `description VARCHAR(500) NULL`
-   `enabled TINYINT NOT NULL DEFAULT 1`
-   `default_template_id BIGINT NULL`
-   `created_at DATETIME NOT NULL`
-   `updated_at DATETIME NOT NULL`

### `form_template`

-   `id BIGINT PK`
-   `template_code VARCHAR(64) NOT NULL`
-   `template_name VARCHAR(150) NOT NULL`
-   `declaration_type_id BIGINT NOT NULL`
-   `version_no INT NOT NULL`
-   `status VARCHAR(20) NOT NULL`（DRAFT/PUBLISHED/DISABLED）
-   `created_by BIGINT NOT NULL`
-   `published_at DATETIME NULL`
-   `created_at DATETIME NOT NULL`
-   `updated_at DATETIME NOT NULL`
-   唯一约束：`(template_code, version_no)`

### `form_field`

-   `id BIGINT PK`
-   `template_id BIGINT NOT NULL`
-   `field_code VARCHAR(64) NOT NULL`
-   `field_label VARCHAR(150) NOT NULL`
-   `field_type VARCHAR(30) NOT NULL`
-   `required TINYINT NOT NULL DEFAULT 0`
-   `min_value DECIMAL(18,2) NULL`
-   `max_value DECIMAL(18,2) NULL`
-   `max_length INT NULL`
-   `options_json JSON NULL`
-   `validation_json JSON NULL`
-   `help_text VARCHAR(255) NULL`
-   `sort_order INT NOT NULL DEFAULT 0`
-   `enabled TINYINT NOT NULL DEFAULT 1`
-   唯一约束：`(template_id, field_code)`

模板发布后，字段定义不可原位修改；应复制为新版本。

### `tax_declaration`

-   `id BIGINT PK`
-   `declaration_no VARCHAR(64) NOT NULL UNIQUE`
-   `taxpayer_id BIGINT NOT NULL`
-   `taxpayer_name_snapshot VARCHAR(200) NOT NULL`
-   `declaration_type_id BIGINT NOT NULL`
-   `declaration_type_snapshot VARCHAR(100) NOT NULL`
-   `template_id BIGINT NOT NULL`
-   `template_version_no INT NOT NULL`
-   `period_start DATE NOT NULL`
-   `period_end DATE NOT NULL`
-   `status VARCHAR(30) NOT NULL`
-   `declared_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00`
-   `form_data JSON NOT NULL`
-   `created_by BIGINT NOT NULL`
-   `submitted_at DATETIME NULL`
-   `last_reviewed_at DATETIME NULL`
-   `version INT NOT NULL DEFAULT 0`（乐观锁，可选）
-   `created_at DATETIME NOT NULL`
-   `updated_at DATETIME NOT NULL`
-   `deleted TINYINT NOT NULL DEFAULT 0`

索引建议： - 唯一索引 `declaration_no`。 -
`(taxpayer_id, declaration_type_id, period_start, period_end, status)`。 -
`(status, submitted_at)`。 - `(created_by, created_at)`。 -
对常用时间范围查询字段建立索引，避免无必要地给每个字段加索引。

注意：重复申报约束不能简单地对所有历史状态做一个无条件唯一索引，因为退回、撤回和更正规则会影响唯一性。V1.0
先在事务内做业务检查，必要时再设计专门的有效申报标记或数据库约束。

### `declaration_review`

-   `id BIGINT PK`
-   `declaration_id BIGINT NOT NULL`
-   `reviewer_id BIGINT NOT NULL`
-   `review_action VARCHAR(20) NOT NULL`（APPROVE/RETURN）
-   `review_comment VARCHAR(1000) NULL`
-   `from_status VARCHAR(30) NOT NULL`
-   `to_status VARCHAR(30) NOT NULL`
-   `reviewed_at DATETIME NOT NULL`
-   `created_at DATETIME NOT NULL`

索引：`(declaration_id, reviewed_at)`、`(reviewer_id, reviewed_at)`。

### `sys_operation_log`

-   `id BIGINT PK`
-   `operator_id BIGINT NULL`
-   `operator_name_snapshot VARCHAR(100) NULL`
-   `operation_type VARCHAR(80) NOT NULL`
-   `target_type VARCHAR(80) NULL`
-   `target_id VARCHAR(80) NULL`
-   `result VARCHAR(20) NOT NULL`
-   `summary VARCHAR(500) NULL`
-   `trace_id VARCHAR(100) NULL`
-   `ip_masked VARCHAR(80) NULL`（仅在合理且必要时保存）
-   `created_at DATETIME NOT NULL`

日志禁止保存密码、认证令牌、完整表单敏感载荷。

### `sys_dict`（可选）

-   `id BIGINT PK`
-   `dict_type VARCHAR(64) NOT NULL`
-   `dict_code VARCHAR(64) NOT NULL`
-   `dict_label VARCHAR(100) NOT NULL`
-   `sort_order INT NOT NULL DEFAULT 0`
-   `enabled TINYINT NOT NULL DEFAULT 1`
-   唯一约束：`(dict_type, dict_code)`

## 7.3 关系概览

-   `sys_user` 与 `sys_role`：多对多，经 `sys_user_role`。
-   `sys_role` 与 `sys_permission`：多对多，经 `sys_role_permission`。
-   `taxpayer` 与 `tax_declaration`：一对多。
-   `declaration_type` 与 `form_template`：一对多。
-   `form_template` 与 `form_field`：一对多。
-   `tax_declaration` 与 `declaration_review`：一对多。
-   `sys_user` 与申报：创建人关联；与审核记录：审核人关联。
-   `tax_declaration` 绑定模板版本，保证历史表单可还原。

数据库 DDL 必须在 `database/migration`
中版本化。初始化演示账号只能放入开发环境种子数据，首次启动后应提示修改密码；不得在生产配置中写入真实秘密。

------------------------------------------------------------------------

# 8. 接口设计初稿

接口以 `/api/v1` 为统一前缀，以下为建议端点，具体 DTO
和字段应在编码前写入 `docs/api-spec.md`。

## 8.1 认证

-   `POST /auth/login`
-   `POST /auth/logout`
-   `GET /auth/me`
-   `PUT /auth/password`

## 8.2 用户与角色

-   `GET /users`
-   `POST /users`
-   `GET /users/{id}`
-   `PUT /users/{id}`
-   `PATCH /users/{id}/status`
-   `POST /users/{id}/reset-password`
-   `GET /roles`
-   `POST /roles`
-   `PUT /roles/{id}`
-   `PUT /roles/{id}/permissions`

## 8.3 纳税人与申报类型

-   `GET /taxpayers`
-   `POST /taxpayers`
-   `GET /taxpayers/{id}`
-   `PUT /taxpayers/{id}`
-   `PATCH /taxpayers/{id}/status`
-   `GET /declaration-types`
-   `POST /declaration-types`
-   `PUT /declaration-types/{id}`

## 8.4 表单模板

-   `GET /form-templates`
-   `POST /form-templates`
-   `GET /form-templates/{id}`
-   `PUT /form-templates/{id}`
-   `POST /form-templates/{id}/fields`
-   `PUT /form-templates/{id}/fields/{fieldId}`
-   `POST /form-templates/{id}/publish`
-   `POST /form-templates/{id}/disable`
-   `GET /form-templates/{id}/preview`

## 8.5 申报

-   `GET /declarations`
-   `POST /declarations`
-   `GET /declarations/{id}`
-   `PUT /declarations/{id}`
-   `POST /declarations/{id}/submit`
-   `POST /declarations/{id}/withdraw`
-   `GET /declarations/{id}/reviews`

## 8.6 审核

-   `GET /reviews/pending`
-   `POST /declarations/{id}/review/approve`
-   `POST /declarations/{id}/review/return`
-   `GET /reviews/history`

## 8.7 统计、查询与审计

-   `GET /statistics/overview`
-   `GET /statistics/declarations/trend`
-   `GET /statistics/declarations/by-status`
-   `GET /statistics/declarations/by-type`
-   `GET /search/declarations`
-   `GET /audit-logs`

所有列表端点应定义分页参数、可筛选字段、可排序字段和最大
pageSize。所有管理端点必须在服务端执行认证与授权。

------------------------------------------------------------------------

# 9. 安全与质量要求

## 9.1 安全基线

-   Spring Security 统一认证与授权。
-   密码仅保存 BCrypt 等强哈希结果。
-   使用 DTO 和验证注解校验输入。
-   ORM 参数绑定，禁止拼接用户输入 SQL。
-   前端输入不能作为可信业务数据。
-   后端重新校验申报状态、金额、模板版本、用户权限。
-   CORS 仅允许开发所需来源，不配置任意来源携带凭据。
-   生产密钥通过环境变量或安全配置注入，不提交 Git。
-   错误信息不泄露堆栈、SQL、密码、Token。
-   审核、导出、权限变更等操作写审计日志。
-   真实个人和税务敏感数据不进入课程演示环境。

## 9.2 非功能需求

-   可用性：常见列表、表单和详情页面操作路径清晰。
-   性能：演示规模下常见列表接口目标响应时间小于 1
    秒（本地环境、合理数据量下的目标，不作生产 SLA 承诺）。
-   可维护性：分层清晰、命名统一、核心状态集中管理。
-   可测试性：Service 层有单元测试，关键 API 有集成测试。
-   可移植性：提供环境版本、数据库初始化与运行说明。
-   可访问性：颜色对比清晰、状态不只依赖颜色表达、表单有标签。
-   一致性：统一分页、日期、金额、错误响应和状态文案。

------------------------------------------------------------------------

# 10. 测试与验收标准

## 10.1 P0 功能验收

1.  使用有效账号登录成功；错误密码登录失败。
2.  停用用户不能登录。
3.  不同角色看到的菜单符合权限配置。
4.  直接调用无权限 API 返回 403，而不是仅在前端隐藏按钮。
5.  可新增、编辑、查询、停用虚构纳税人。
6.  可创建申报草稿并保存；刷新后数据仍存在。
7.  必填字段缺失时，前端有提示且后端拒绝提交。
8.  合法申报提交后状态变为待审核，并生成唯一申报编号。
9.  待审核申报不能由普通经办人编辑。
10. 审核人员可以通过申报；状态与审核记录同时保存。
11. 审核人员可以退回申报；退回意见必填，记录可查。
12. 同一申报重复审核时，第二次请求失败并返回合理冲突响应。
13. 已通过申报不能被直接编辑。
14. 统计数字与查询结果一致。
15. 组合筛选、分页、排序正确。
16. 导出结果与当前筛选条件一致，且不超出授权范围。
17. 操作日志记录关键操作，不包含密码和 Token。
18. 无数据、接口失败和无权限页面有合理反馈。
19. 项目按 README 步骤可在干净环境启动。
20. 不存在仅有外观、点击无效的核心按钮。

## 10.2 推荐测试用例

  编号        场景                     预期结果
  ----------- ------------------------ --------------------------
  AUTH-01     正确账号密码             登录成功
  AUTH-02     错误密码                 返回通用认证失败信息
  AUTH-03     停用用户登录             拒绝登录
  PERM-01     经办人访问用户管理 API   403
  TAX-01      创建重复纳税人编号       业务校验失败
  FORM-01     发布缺少必填标签的模板   发布失败并说明原因
  DECL-01     保存合法草稿             数据持久化且状态为 DRAFT
  DECL-02     缺少必填字段提交         后端拒绝
  DECL-03     合法提交                 状态为 PENDING_REVIEW
  DECL-04     重复提交同一请求         不产生重复申报
  REVIEW-01   审核待审核申报           状态和审核记录一致
  REVIEW-02   审核已通过申报           返回状态冲突
  REVIEW-03   经办人审核自己的申报     拒绝
  STAT-01     统计总量与筛选列表核对   口径一致
  SEARCH-01   多条件分页查询           条件和总数正确
  AUDIT-01    审核/导出操作            日志存在且不泄露敏感值

## 10.3 验收证据

-   运行环境与版本截图。
-   登录、工作台、申报、审核、统计、权限配置等关键页面截图。
-   API 调试结果或自动化测试报告。
-   数据库 ER 图与主要表结构。
-   README 启动步骤和成功运行日志。
-   UI 参考调研记录。
-   已知限制清单。

------------------------------------------------------------------------

# 11. 本地部署与运行

## 11.1 环境

-   JDK 17。
-   Node.js LTS（建议 20 或项目创建时与依赖兼容的 LTS 版本）。
-   MySQL 8.0 系列。
-   Maven（优先使用 Maven Wrapper）。
-   Git。

## 11.2 配置

-   提供 `.env.example` 或配置模板，不包含真实密码。
-   数据库连接通过环境变量或本地未提交配置文件提供。
-   前端 API 基地址通过 Vite 环境配置管理。
-   开发环境和测试环境配置分离。
-   通过数据库迁移脚本建表，通过 seed 脚本加载虚构演示数据。

## 11.3 启动流程

1.  创建本地数据库。
2.  配置数据库连接。
3.  执行数据库迁移。
4.  执行开发环境种子数据脚本。
5.  启动后端服务。
6.  安装前端依赖并启动开发服务器。
7.  使用 README 提供的演示账号登录。
8.  首次登录修改演示密码。
9.  执行核心流程验收。

README
必须明确端口、启动命令、初始化方式、演示账号、常见错误和重置演示数据的方法。演示账号只能使用明显的虚构账号，不得复用个人密码。

------------------------------------------------------------------------

# 12. 开发阶段计划

## 阶段 0：项目基线

-   确认课程要求、JDK、数据库与交付格式。
-   完成 UI 参考调研。
-   生成 ER 图、API 草案和页面清单。
-   建立 Git 仓库和 docs。

交付：需求文档、UI 调研、技术选型、ER 图草案。

## 阶段 1：工程骨架

-   创建前后端工程。
-   统一响应、异常、分页、日志、配置。
-   建立数据库迁移机制。
-   完成健康检查与 README 初稿。

交付：空工程可启动，数据库可连接。

## 阶段 2：认证与权限

-   登录、退出、当前用户信息。
-   用户、角色、权限。
-   后端 API 授权与前端动态菜单。

交付：权限矩阵测试通过。

## 阶段 3：纳税人与表单

-   纳税人管理。
-   申报类型。
-   模板、字段、模板版本、预览。

交付：可配置并发布模拟表单。

## 阶段 4：申报与审核闭环

-   草稿、保存、提交、退回、重新提交、审核通过、撤回。
-   状态机、事务、并发控制、审核历史。

交付：核心业务流程可完整演示。

## 阶段 5：统计、查询、日志

-   首页指标和图表。
-   组合查询、分页、导出。
-   操作审计。

交付：统计与业务数据口径一致。

## 阶段 6：质量与验收

-   自动化测试、异常测试、权限测试。
-   修复 UI 细节。
-   整理截图、数据库文档和演示脚本。

交付：可运行版本与验收材料。

------------------------------------------------------------------------

# 13. 风险与待确认事项

1.  课程是否指定必须使用 JSP/Servlet，当前按允许 Spring Boot 处理。
2.  是否要求 WAR 部署到外部 Tomcat；当前默认 Spring Boot 可执行 JAR。
3.  是否必须使用指定数据库；当前默认 MySQL 8.0。
4.  是否要求导出 Excel；V1.0 优先 CSV，Excel 可作为 P1。
5.  表单是否要求真正的可视化拖拽设计器；V1.0
    仅要求字段配置与预览，不默认引入复杂拖拽引擎。
6.  数据范围是否要区分不同部门；V1.0 先采用角色权限与简单授权范围。
7.  项目是否必须提供 UML、用例图、类图、ER
    图；如课程要求，应在后续补齐。
8.  实际依赖版本应在实施时根据官方文档锁定并通过构建测试。

------------------------------------------------------------------------

# 14. 版本记录

  ------------------------------------------------------------------------------------------------------
  版本                    日期                    说明
  ----------------------- ----------------------- ------------------------------------------------------
  V1.0                    2026-10-29              建立项目范围、功能需求、UI
                                                  规范、技术架构、数据库初稿、接口清单、测试与验收基线

  ------------------------------------------------------------------------------------------------------

## V1.1 建议范围

-   根据课程 rubric 补齐 UML、用例图和类图。
-   根据实际 UI 调研结果确定最终页面设计。
-   将数据库字段、索引和外键落成完整 DDL。
-   细化每个 API 的请求/响应 JSON、错误码和权限代码。
-   根据真实开发结果更新已完成/未完成清单。
