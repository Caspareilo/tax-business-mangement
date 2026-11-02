-- 税务业务管理系统 演示数据（全部为虚构数据，禁止用于生产）
-- 依赖 database/schema.sql，执行顺序：schema.sql -> seed-demo.sql
-- 演示账号统一密码见 README，密码仅以 BCrypt 摘要形式保存。

USE tax_demo;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------
-- 角色
-- ---------------------------------------------------------------
INSERT INTO sys_role (id, role_code, role_name, description, status, created_at, updated_at) VALUES
(1, 'ADMIN',    '系统管理员', '维护用户、角色、字典与基础数据，不参与申报审核', 'ACTIVE', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(2, 'CLERK',    '申报经办员', '录入、保存、提交和撤回本人申报', 'ACTIVE', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(3, 'REVIEWER', '申报审核员', '审核待办申报，填写审核意见', 'ACTIVE', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(4, 'ANALYST',  '统计分析员', '查看统计报表并导出申报数据', 'ACTIVE', '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
(5, 'VIEWER',   '只读访客', '仅查看工作台与申报列表', 'ACTIVE', '2026-01-01 09:00:00', '2026-01-01 09:00:00');

-- ---------------------------------------------------------------
-- 权限点
-- ---------------------------------------------------------------
INSERT INTO sys_permission (id, permission_code, permission_name, group_name, permission_type, parent_id, url_pattern, http_method, sort_order, status) VALUES
(1,  'dashboard:view',       '访问工作台',     '工作台',   'MENU',   NULL, '/dashboard',            'GET',  10,  'ACTIVE'),
(2,  'user:manage',          '用户管理',       '系统管理', 'MENU',   NULL, '/admin/users',          'GET',  20,  'ACTIVE'),
(3,  'role:manage',          '角色权限管理',   '系统管理', 'MENU',   NULL, '/admin/roles',          'GET',  30,  'ACTIVE'),
(4,  'taxpayer:view',        '查询纳税人',     '纳税人',   'MENU',   NULL, '/taxpayers',            'GET',  40,  'ACTIVE'),
(5,  'taxpayer:manage',      '维护纳税人',     '纳税人',   'ACTION', NULL, '/taxpayers/save',       'POST', 50,  'ACTIVE'),
(6,  'type:manage',          '维护申报类型',   '申报配置', 'ACTION', NULL, '/declaration-types/save', 'POST', 60, 'ACTIVE'),
(7,  'template:manage',      '维护表单模板',   '申报配置', 'MENU',   NULL, '/form-templates',       'GET',  70,  'ACTIVE'),
(8,  'declaration:view',     '查询申报',       '申报业务', 'MENU',   NULL, '/declarations',         'GET',  80,  'ACTIVE'),
(9,  'declaration:create',   '填报申报',       '申报业务', 'ACTION', NULL, '/declarations/create',  'GET',  90,  'ACTIVE'),
(10, 'declaration:submit',   '提交/撤回申报',  '申报业务', 'ACTION', NULL, '/declarations/submit',  'POST', 100, 'ACTIVE'),
(11, 'review:manage',        '审核申报',       '申报审核', 'MENU',   NULL, '/reviews',              'GET',  110, 'ACTIVE'),
(12, 'statistics:view',      '查看统计报表',   '统计分析', 'MENU',   NULL, '/statistics',           'GET',  120, 'ACTIVE'),
(13, 'audit:view',           '查看操作审计',   '系统管理', 'MENU',   NULL, '/admin/audit-logs',     'GET',  130, 'ACTIVE'),
(14, 'dict:manage',          '维护数据字典',   '系统管理', 'MENU',   NULL, '/admin/dictionaries',   'GET',  140, 'ACTIVE'),
(15, 'export:declaration',   '导出申报数据',   '申报业务', 'ACTION', NULL, '/search/export',        'POST', 150, 'ACTIVE');

-- ---------------------------------------------------------------
-- 用户（BCrypt cost=10）
-- admin / Admin@123        clerk1 / Clerk@123
-- reviewer1 / Reviewer@123 analyst1 / Analyst@123
-- viewer1 / Viewer@123     disabled1 / Disabled@123（停用）
-- ---------------------------------------------------------------
INSERT INTO sys_user (id, username, password_hash, display_name, status, last_login_at, created_at, updated_at, deleted) VALUES
(1, 'admin',     '$2a$10$i8IlSJxqTCz2YjdOpopNmeFRii1zx.1gU22ka/IE.BxqlD2vtWDtK', '系统管理员', 'ACTIVE',   '2026-10-28 08:45:00', '2026-01-01 09:00:00', '2026-10-28 08:45:00', 0),
(2, 'clerk1',    '$2a$10$OeGaUZlGK/SDNNqnXk4.LeZVeLdQwOtdNGvXcvWqlcKNZ9il8oeJC', '张经办',     'ACTIVE',   '2026-10-29 08:30:00', '2026-01-05 10:00:00', '2026-10-29 08:30:00', 0),
(3, 'reviewer1', '$2a$10$J.rWQULOAdVVCKBVld71deLaBWw3MIMlj0BAW12y.hhZe0e0iaa5S', '李审核',     'ACTIVE',   '2026-10-29 09:10:00', '2026-01-05 10:10:00', '2026-10-29 09:10:00', 0),
(4, 'analyst1',  '$2a$10$dMHHvil41TXi5lrUahKrguMrMIV.alm9E/JGSIaxYFIqXhMaTWLqi', '王分析',     'ACTIVE',   '2026-10-27 16:20:00', '2026-02-01 09:30:00', '2026-10-27 16:20:00', 0),
(5, 'viewer1',   '$2a$10$qopJKOWK1HR41G03cRhH2OIbRnYhtDyWGpQbaHPfBdvyYnH/o5Q.S', '赵查看',     'ACTIVE',   '2026-10-26 14:05:00', '2026-03-01 09:30:00', '2026-10-26 14:05:00', 0),
(6, 'disabled1', '$2a$10$FakRmqG0IC1tP1w4ujnIyuBLTEhgGZM4FHJmEmN.1fo8QtJzIKWtC', '停用演示账号', 'DISABLED', '2026-06-01 09:00:00', '2026-03-01 10:00:00', '2026-07-01 09:00:00', 0);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 5);

-- ADMIN 有意不授予 review:manage，避免管理员无审核角色时代替审核员审核
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(1, 12), (1, 13), (1, 14), (1, 15),
(2, 1), (2, 4), (2, 8), (2, 9), (2, 10),
(3, 1), (3, 4), (3, 8), (3, 11),
(4, 1), (4, 4), (4, 8), (4, 12), (4, 15),
(5, 1), (5, 8);

-- ---------------------------------------------------------------
-- 纳税人（虚构名称与 000 号码）
-- ---------------------------------------------------------------
INSERT INTO taxpayer (id, taxpayer_code, taxpayer_name, taxpayer_type, contact_name, contact_phone, registration_status, remark, created_at, updated_at, deleted) VALUES
(1, 'DEMO-COMP-001', '晨星数字科技（演示）有限公司', 'COMPANY',     '陈晨', '000-0000-0001', 'ACTIVE',   '软件开发与技术服务', '2026-01-10 09:00:00', '2026-01-10 09:00:00', 0),
(2, 'DEMO-COMP-002', '青禾供应链（演示）有限公司',   'COMPANY',     '周青', '000-0000-0002', 'ACTIVE',   '仓储与运输服务',     '2026-01-12 09:00:00', '2026-01-12 09:00:00', 0),
(3, 'DEMO-IND-001',  '林小满（演示个体户）',         'INDIVIDUAL',  '林小满', '000-0000-0003', 'ACTIVE',   '设计服务个体工商户', '2026-02-01 09:00:00', '2026-02-01 09:00:00', 0),
(4, 'DEMO-INST-001', '明德职业培训学校（演示）',     'INSTITUTION', '许明', '000-0000-0004', 'ACTIVE',   '非营利职业技能培训', '2026-02-15 09:00:00', '2026-02-15 09:00:00', 0),
(5, 'DEMO-COMP-003', '微光文化传媒（演示）有限公司', 'COMPANY',     '罗微', '000-0000-0005', 'DISABLED', '已停用，仅保留历史数据', '2026-03-01 09:00:00', '2026-07-01 10:00:00', 0),
(6, 'DEMO-COMP-004', '远山智能制造（演示）有限公司', 'COMPANY',     '高远', '000-0000-0006', 'ACTIVE',   '智能设备零部件制造', '2026-03-18 09:00:00', '2026-03-18 09:00:00', 0);

-- ---------------------------------------------------------------
-- 申报类型
-- ---------------------------------------------------------------
INSERT INTO declaration_type (id, type_code, type_name, description, enabled, deadline_day, created_at, updated_at) VALUES
(1, 'VAT',   '增值税及附加税费申报', '按月或按季申报增值税及附加税费', 1, 15, '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
(2, 'CIT',   '企业所得税预缴申报',   '企业按季度预缴企业所得税',       1, 15, '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
(3, 'IIT',   '个人所得税经营所得申报', '个体工商户、个人独资企业申报经营所得', 1, 15, '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
(4, 'STAMP', '财产和行为税合并申报', '印花税、房产税等合并申报',       1, 15, '2026-01-02 09:00:00', '2026-01-02 09:00:00');

-- ---------------------------------------------------------------
-- 表单模板与版本
-- ---------------------------------------------------------------
INSERT INTO form_template (id, template_code, template_name, declaration_type_id, version_no, status, created_by, published_at, created_at, updated_at) VALUES
(1, 'VAT-2026',   '增值税及附加税费申报表', 1, 1, 'PUBLISHED', 1, '2026-01-05 10:00:00', '2026-01-05 09:30:00', '2026-01-05 10:00:00'),
(2, 'VAT-2026',   '增值税及附加税费申报表（2026 年 10 月调整稿）', 1, 2, 'DRAFT', 1, NULL, '2026-10-20 15:00:00', '2026-10-20 15:00:00'),
(3, 'CIT-2026',   '企业所得税预缴申报表', 2, 1, 'PUBLISHED', 1, '2026-01-06 10:00:00', '2026-01-06 09:30:00', '2026-01-06 10:00:00'),
(4, 'IIT-2026',   '个人所得税经营所得申报表', 3, 1, 'PUBLISHED', 1, '2026-01-06 11:00:00', '2026-01-06 10:30:00', '2026-01-06 11:00:00'),
(5, 'STAMP-2026', '财产和行为税合并申报表', 4, 1, 'PUBLISHED', 1, '2026-01-07 10:00:00', '2026-01-07 09:30:00', '2026-01-07 10:00:00');

INSERT INTO form_field (id, template_id, field_code, field_label, field_type, required, min_value, max_value, max_length, options_json, help_text, sort_order, enabled) VALUES
(1,  1, 'sales_amount',      '销售额（不含税）',     'AMOUNT',  1, 0.00, 999999999.99, NULL, NULL, '单位为元，最多两位小数', 1, 1),
(2,  1, 'deductible_amount', '本期可抵扣金额',       'AMOUNT',  1, 0.00, 999999999.99, NULL, NULL, '无抵扣时填 0', 2, 1),
(3,  1, 'tax_method',        '计税方式',             'RADIO',   1, NULL, NULL, NULL, '["一般计税","简易计税"]', '按主管税务机关认定方式选择', 3, 1),
(4,  1, 'declaration_cycle', '申报周期',             'RADIO',   1, NULL, NULL, NULL, '["月度","季度"]', '与税种认定信息保持一致', 4, 1),
(5,  1, 'has_contract',      '是否存在合同',         'BOOLEAN', 1, NULL, NULL, NULL, NULL, '选择“是”时请留存合同备查', 5, 1),
(6,  1, 'remark',            '备注',                 'TEXT',    0, NULL, NULL, 500, NULL, '可填写需要说明的事项', 6, 1),
(7,  2, 'sales_amount',      '销售额（不含税）',     'AMOUNT',  1, 0.00, 999999999.99, NULL, NULL, '调整稿字段示例', 1, 1),
(8,  2, 'remark',            '备注',                 'TEXT',    0, NULL, NULL, 500, NULL, '', 2, 1),
(9,  3, 'profit_total',      '利润总额',             'AMOUNT',  1, 0.00, 999999999.99, NULL, NULL, '按会计核算口径填写', 1, 1),
(10, 3, 'tax_rate',          '适用税率',             'RADIO',   1, NULL, NULL, NULL, '["25%","20%","15%"]', '小型微利企业请按认定结果选择', 2, 1),
(11, 3, 'employee_count',    '从业人数',             'INTEGER', 1, 0.00, 100000.00, NULL, NULL, '填写本季度平均从业人数', 3, 1),
(12, 3, 'prepay_date',       '预缴日期',             'DATE',    1, NULL, NULL, NULL, NULL, '格式 yyyy-MM-dd', 4, 1),
(13, 3, 'industry',          '所属行业',             'TEXT',    1, NULL, NULL, 100, NULL, '', 5, 1),
(14, 3, 'remark',            '备注',                 'TEXT',    0, NULL, NULL, 500, NULL, '', 6, 1),
(15, 4, 'income_total',      '收入总额',             'AMOUNT',  1, 0.00, 999999999.99, NULL, NULL, '本年度累计收入', 1, 1),
(16, 4, 'deduction_total',   '成本费用',             'AMOUNT',  1, 0.00, 999999999.99, NULL, NULL, '本年度累计成本费用', 2, 1),
(17, 4, 'income_type',       '所得类型',             'RADIO',   1, NULL, NULL, NULL, '["经营所得","劳务报酬","稿酬所得"]', '', 3, 1),
(18, 4, 'has_special_deduction', '是否享受专项附加扣除', 'BOOLEAN', 1, NULL, NULL, NULL, NULL, '', 4, 1),
(19, 4, 'declare_date',      '申报日期',             'DATE',    1, NULL, NULL, NULL, NULL, '格式 yyyy-MM-dd', 5, 1),
(20, 5, 'contract_amount',   '合同金额',             'AMOUNT',  1, 0.00, 999999999.99, NULL, NULL, '同一合同只申报一次', 1, 1),
(21, 5, 'contract_type',     '合同类型',             'RADIO',   1, NULL, NULL, NULL, '["买卖合同","借款合同","租赁合同","产权转移书据"]', '', 2, 1),
(22, 5, 'tax_items',         '应税项目',             'MULTI',   1, NULL, NULL, NULL, '["印花税","房产税","城镇土地使用税"]', '可多选', 3, 1),
(23, 5, 'remark',            '备注',                 'TEXT',    0, NULL, NULL, 500, NULL, '', 4, 1);

-- ---------------------------------------------------------------
-- 申报单（覆盖 DRAFT / PENDING_REVIEW / RETURNED / APPROVED / WITHDRAWN）
-- ---------------------------------------------------------------
INSERT INTO tax_declaration (id, declaration_no, taxpayer_id, taxpayer_name_snapshot, declaration_type_id, declaration_type_snapshot, template_id, template_version_no, period_start, period_end, status, declared_amount, form_data_json, created_by, submitted_at, last_reviewed_at, version, created_at, updated_at, deleted) VALUES
(1,  'TD20261008-000001', 1, '晨星数字科技（演示）有限公司',   1, '增值税及附加税费申报', 1, 1, '2026-09-01', '2026-09-30', 'DRAFT',          120000.00,
 JSON_OBJECT('sales_amount', '100000.00', 'deductible_amount', '20000.00', 'tax_method', '一般计税', 'declaration_cycle', '月度', 'has_contract', 'true', 'remark', '待补充合同编号'),
 2, NULL, NULL, 0, '2026-10-08 10:20:00', '2026-10-08 10:20:00', 0),
(2,  'TD20261009-000002', 1, '晨星数字科技（演示）有限公司',   1, '增值税及附加税费申报', 1, 1, '2026-08-01', '2026-08-31', 'PENDING_REVIEW', 135000.00,
 JSON_OBJECT('sales_amount', '120000.00', 'deductible_amount', '15000.00', 'tax_method', '一般计税', 'declaration_cycle', '月度', 'has_contract', 'true', 'remark', '八月申报'),
 2, '2026-10-10 09:30:00', NULL, 1, '2026-10-09 16:40:00', '2026-10-10 09:30:00', 0),
(3,  'TD20260910-000003', 2, '青禾供应链（演示）有限公司',     1, '增值税及附加税费申报', 1, 1, '2026-08-01', '2026-08-31', 'RETURNED',        88000.00,
 JSON_OBJECT('sales_amount', '96000.00', 'deductible_amount', '8000.00', 'tax_method', '一般计税', 'declaration_cycle', '月度', 'has_contract', 'false', 'remark', '运输服务收入'),
 2, '2026-09-10 11:00:00', '2026-09-15 14:20:00', 3, '2026-09-09 15:10:00', '2026-09-15 14:20:00', 0),
(4,  'TD20260916-000004', 3, '林小满（演示个体户）',           1, '增值税及附加税费申报', 1, 1, '2026-07-01', '2026-07-31', 'APPROVED',        56000.00,
 JSON_OBJECT('sales_amount', '60000.00', 'deductible_amount', '4000.00', 'tax_method', '简易计税', 'declaration_cycle', '季度', 'has_contract', 'true', 'remark', '设计服务'),
 2, '2026-09-16 09:20:00', '2026-09-18 10:05:00', 2, '2026-09-15 17:30:00', '2026-09-18 10:05:00', 0),
(5,  'TD20260920-000005', 4, '明德职业培训学校（演示）',       2, '企业所得税预缴申报',   3, 1, '2026-01-01', '2026-06-30', 'WITHDRAWN',       230000.00,
 JSON_OBJECT('profit_total', '230000.00', 'tax_rate', '20%', 'employee_count', '36', 'prepay_date', '2026-09-20', 'industry', '职业技能培训', 'remark', '发现人数填错后撤回'),
 2, '2026-09-20 10:10:00', NULL, 3, '2026-09-19 14:00:00', '2026-09-21 09:15:00', 0),
(6,  'TD20261012-000006', 2, '青禾供应链（演示）有限公司',     2, '企业所得税预缴申报',   3, 1, '2026-01-01', '2026-06-30', 'PENDING_REVIEW',  410000.00,
 JSON_OBJECT('profit_total', '410000.00', 'tax_rate', '25%', 'employee_count', '88', 'prepay_date', '2026-10-12', 'industry', '仓储和邮政业', 'remark', ''),
 2, '2026-10-12 15:45:00', NULL, 1, '2026-10-12 11:20:00', '2026-10-12 15:45:00', 0),
(7,  'TD20261014-000007', 1, '晨星数字科技（演示）有限公司',   3, '个人所得税经营所得申报', 4, 1, '2026-06-01', '2026-06-30', 'APPROVED',        98000.00,
 JSON_OBJECT('income_total', '120000.00', 'deduction_total', '22000.00', 'income_type', '经营所得', 'has_special_deduction', 'true', 'declare_date', '2026-10-14'),
 2, '2026-10-14 09:05:00', '2026-10-16 16:30:00', 2, '2026-10-13 17:50:00', '2026-10-16 16:30:00', 0),
(8,  'TD20261018-000008', 4, '明德职业培训学校（演示）',       4, '财产和行为税合并申报', 5, 1, '2026-09-01', '2026-09-30', 'PENDING_REVIEW',   31000.00,
 JSON_OBJECT('contract_amount', '31000.00', 'contract_type', '租赁合同', 'tax_items', '印花税,房产税', 'remark', '实训场地租赁'),
 2, '2026-10-18 10:00:00', NULL, 1, '2026-10-17 15:20:00', '2026-10-18 10:00:00', 0),
(9,  'TD20261020-000009', 6, '远山智能制造（演示）有限公司',   1, '增值税及附加税费申报', 1, 1, '2026-10-01', '2026-10-31', 'DRAFT',            75000.00,
 JSON_OBJECT('sales_amount', '80000.00', 'deductible_amount', '5000.00', 'tax_method', '一般计税', 'declaration_cycle', '月度', 'has_contract', 'true', 'remark', '十月申报草稿'),
 2, NULL, NULL, 0, '2026-10-20 09:40:00', '2026-10-20 09:40:00', 0),
(10, 'TD20260820-000010', 6, '远山智能制造（演示）有限公司',   2, '企业所得税预缴申报',   3, 1, '2026-01-01', '2026-06-30', 'APPROVED',       512000.00,
 JSON_OBJECT('profit_total', '512000.00', 'tax_rate', '25%', 'employee_count', '152', 'prepay_date', '2026-08-20', 'industry', '通用设备制造业', 'remark', ''),
 2, '2026-08-20 10:30:00', '2026-08-25 09:50:00', 2, '2026-08-19 16:10:00', '2026-08-25 09:50:00', 0),
(11, 'TD20260805-000011', 5, '微光文化传媒（演示）有限公司',   1, '增值税及附加税费申报', 1, 1, '2026-06-01', '2026-06-30', 'APPROVED',        66000.00,
 JSON_OBJECT('sales_amount', '72000.00', 'deductible_amount', '6000.00', 'tax_method', '一般计税', 'declaration_cycle', '季度', 'has_contract', 'true', 'remark', '停用前历史申报'),
 2, '2026-08-05 11:10:00', '2026-08-12 15:40:00', 2, '2026-08-04 14:30:00', '2026-08-12 15:40:00', 0),
(12, 'TD20261003-000012', 6, '远山智能制造（演示）有限公司',   3, '个人所得税经营所得申报', 4, 1, '2026-08-01', '2026-08-31', 'RETURNED',        87000.00,
 JSON_OBJECT('income_total', '100000.00', 'deduction_total', '13000.00', 'income_type', '劳务报酬', 'has_special_deduction', 'false', 'declare_date', '2026-10-03'),
 2, '2026-10-03 09:15:00', '2026-10-05 11:25:00', 3, '2026-10-02 16:00:00', '2026-10-05 11:25:00', 0);

-- ---------------------------------------------------------------
-- 审核记录（只对应已通过/已退回的申报）
-- ---------------------------------------------------------------
INSERT INTO declaration_review (id, declaration_id, reviewer_id, review_action, review_comment, from_status, to_status, reviewed_at, created_at) VALUES
(1, 3,  3, 'RETURN',  '附表金额与主表不一致，请核对后重新提交。', 'PENDING_REVIEW', 'RETURNED', '2026-09-15 14:20:00', '2026-09-15 14:20:00'),
(2, 4,  3, 'APPROVE', '资料齐全，金额核对无误。',               'PENDING_REVIEW', 'APPROVED', '2026-09-18 10:05:00', '2026-09-18 10:05:00'),
(3, 7,  3, 'APPROVE', '专项附加扣除信息完整。',                 'PENDING_REVIEW', 'APPROVED', '2026-10-16 16:30:00', '2026-10-16 16:30:00'),
(4, 10, 3, 'APPROVE', '预缴数据与财务报表一致。',               'PENDING_REVIEW', 'APPROVED', '2026-08-25 09:50:00', '2026-08-25 09:50:00'),
(5, 11, 3, 'APPROVE', '历史申报，资料归档。',                   'PENDING_REVIEW', 'APPROVED', '2026-08-12 15:40:00', '2026-08-12 15:40:00'),
(6, 12, 3, 'RETURN',  '劳务报酬所得类型与附报资料不一致，请核实。', 'PENDING_REVIEW', 'RETURNED', '2026-10-05 11:25:00', '2026-10-05 11:25:00');

-- ---------------------------------------------------------------
-- 操作审计（仅为演示，不含密码、会话或令牌）
-- ---------------------------------------------------------------
INSERT INTO sys_operation_log (id, operator_id, operator_name_snapshot, operation_type, target_type, target_id, result, summary, trace_id, created_at) VALUES
(1,  2, '张经办',     'LOGIN',               'SESSION',     '2',      'SUCCESS', '登录成功', 'demo-trace-0001', '2026-10-29 08:30:00'),
(2,  3, '李审核',     'LOGIN',               'SESSION',     '3',      'SUCCESS', '登录成功', 'demo-trace-0002', '2026-10-29 09:10:00'),
(3,  2, '张经办',     'DECLARATION_SAVE',    'DECLARATION', '1',      'SUCCESS', '保存申报草稿 TD20261008-000001', 'demo-trace-0003', '2026-10-08 10:20:00'),
(4,  2, '张经办',     'DECLARATION_SUBMIT',  'DECLARATION', '2',      'SUCCESS', '提交申报 TD20261009-000002', 'demo-trace-0004', '2026-10-10 09:30:00'),
(5,  3, '李审核',     'DECLARATION_RETURN',  'DECLARATION', '3',      'SUCCESS', '退回申报 TD20260910-000003', 'demo-trace-0005', '2026-09-15 14:20:00'),
(6,  3, '李审核',     'DECLARATION_APPROVE', 'DECLARATION', '4',      'SUCCESS', '审核通过 TD20260916-000004', 'demo-trace-0006', '2026-09-18 10:05:00'),
(7,  2, '张经办',     'DECLARATION_WITHDRAW', 'DECLARATION', '5',     'SUCCESS', '撤回申报 TD20260920-000005', 'demo-trace-0007', '2026-09-21 09:15:00'),
(8,  4, '王分析',     'DECLARATION_EXPORT',  'DECLARATION', NULL,     'SUCCESS', '导出申报数据 4 条', 'demo-trace-0008', '2026-10-27 16:25:00'),
(9,  NULL, '未知账号', 'LOGIN',               'SESSION',     NULL,     'FAILURE', '登录失败：账号不存在或密码错误', 'demo-trace-0009', '2026-10-28 21:40:00'),
(10, NULL, 'disabled1', 'LOGIN',              'SESSION',     '6',      'FAILURE', '登录失败：账号已停用 disabled1', 'demo-trace-0010', '2026-10-29 07:50:00');

-- ---------------------------------------------------------------
-- 数据字典
-- ---------------------------------------------------------------
INSERT INTO sys_dict (id, dict_type, dict_key, dict_label, sort_order, enabled) VALUES
(1,  'taxpayer_type',     'COMPANY',     '企业',       10, 1),
(2,  'taxpayer_type',     'INDIVIDUAL',  '个体工商户', 20, 1),
(3,  'taxpayer_type',     'INSTITUTION', '事业单位',   30, 1),
(4,  'declaration_status', 'DRAFT',          '草稿',     10, 1),
(5,  'declaration_status', 'PENDING_REVIEW', '待审核',   20, 1),
(6,  'declaration_status', 'RETURNED',       '已退回',   30, 1),
(7,  'declaration_status', 'APPROVED',       '已通过',   40, 1),
(8,  'declaration_status', 'WITHDRAWN',      '已撤回',   50, 1),
(9,  'review_action',      'APPROVE',        '审核通过', 10, 1),
(10, 'review_action',      'RETURN',         '退回修改', 20, 1),
(11, 'declaration_cycle',  'MONTHLY',        '月度',     10, 1),
(12, 'declaration_cycle',  'QUARTERLY',      '季度',     20, 1);
