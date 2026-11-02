-- 演示种子数据：全部为虚构数据，仅用于教学。演示密码均为 Demo@1234（BCrypt）。
INSERT INTO sys_role (id, role_code, role_name, description, status) VALUES
(1, 'ADMIN', '系统管理员', '系统配置与用户权限管理', 'ACTIVE'),
(2, 'CLERK', '业务经办人', '创建、编辑并提交本人申报', 'ACTIVE'),
(3, 'REVIEWER', '审核人员', '审核申报并填写意见', 'ACTIVE'),
(4, 'ANALYST', '统计查看人员', '查看与导出统计结果', 'ACTIVE'),
(5, 'VIEWER', '演示只读用户', '只读查看演示数据', 'ACTIVE');

INSERT INTO sys_permission (id, permission_code, permission_name, permission_type, parent_id, route_path, api_pattern, http_method, sort_order, status) VALUES
(1,  'dashboard:view',        '工作台',       'MENU', NULL, '/dashboard',          NULL, NULL, 1,  'ACTIVE'),
(2,  'user:menu',             '用户管理',     'MENU', NULL, '/users',              NULL, NULL, 2,  'ACTIVE'),
(3,  'role:menu',             '角色权限',     'MENU', NULL, '/roles',              NULL, NULL, 3,  'ACTIVE'),
(4,  'taxpayer:menu',         '纳税人管理',   'MENU', NULL, '/taxpayers',          NULL, NULL, 4,  'ACTIVE'),
(5,  'declaration-type:menu', '申报类型',     'MENU', NULL, '/declaration-types',  NULL, NULL, 5,  'ACTIVE'),
(6,  'form-template:menu',    '表单模板',     'MENU', NULL, '/form-templates',     NULL, NULL, 6,  'ACTIVE'),
(7,  'declaration:menu',      '申报管理',     'MENU', NULL, '/declarations',       NULL, NULL, 7,  'ACTIVE'),
(8,  'review:menu',           '审核管理',     'MENU', NULL, '/reviews',            NULL, NULL, 8,  'ACTIVE'),
(9,  'search:menu',           '综合查询',     'MENU', NULL, '/search',             NULL, NULL, 9,  'ACTIVE'),
(10, 'statistics:menu',       '统计分析',     'MENU', NULL, '/statistics',         NULL, NULL, 10, 'ACTIVE'),
(11, 'audit:menu',            '操作日志',     'MENU', NULL, '/audit-logs',         NULL, NULL, 11, 'ACTIVE'),
(12, 'user:read',             '查看用户',     'API', NULL, NULL, '/api/v1/users', 'GET', 12, 'ACTIVE'),
(13, 'user:write',            '管理用户',     'API', NULL, NULL, '/api/v1/users/**', 'POST', 13, 'ACTIVE'),
(14, 'role:read',             '查看角色',     'API', NULL, NULL, '/api/v1/roles', 'GET', 14, 'ACTIVE'),
(15, 'role:write',            '管理角色权限', 'API', NULL, NULL, '/api/v1/roles/**', 'PUT', 15, 'ACTIVE'),
(16, 'taxpayer:read',         '查看纳税人',   'API', NULL, NULL, '/api/v1/taxpayers', 'GET', 16, 'ACTIVE'),
(17, 'taxpayer:write',        '管理纳税人',   'API', NULL, NULL, '/api/v1/taxpayers/**', 'POST', 17, 'ACTIVE'),
(18, 'declaration-type:read', '查看申报类型', 'API', NULL, NULL, '/api/v1/declaration-types', 'GET', 18, 'ACTIVE'),
(19, 'declaration-type:write','管理申报类型', 'API', NULL, NULL, '/api/v1/declaration-types/**', 'POST', 19, 'ACTIVE'),
(20, 'form-template:read',    '查看表单模板', 'API', NULL, NULL, '/api/v1/form-templates', 'GET', 20, 'ACTIVE'),
(21, 'form-template:write',   '管理表单模板', 'API', NULL, NULL, '/api/v1/form-templates/**', 'POST', 21, 'ACTIVE'),
(22, 'declaration:read',      '查看申报',     'API', NULL, NULL, '/api/v1/declarations', 'GET', 22, 'ACTIVE'),
(23, 'declaration:write',     '编辑申报',     'API', NULL, NULL, '/api/v1/declarations/**', 'PUT', 23, 'ACTIVE'),
(24, 'declaration:submit',    '提交申报',     'API', NULL, NULL, '/api/v1/declarations/*/submit', 'POST', 24, 'ACTIVE'),
(25, 'declaration:withdraw',  '撤回申报',     'API', NULL, NULL, '/api/v1/declarations/*/withdraw', 'POST', 25, 'ACTIVE'),
(26, 'declaration:export',    '导出申报',     'API', NULL, NULL, '/api/v1/search/declarations/export', 'GET', 26, 'ACTIVE'),
(27, 'review:read',           '查看审核',     'API', NULL, NULL, '/api/v1/reviews', 'GET', 27, 'ACTIVE'),
(28, 'review:write',          '审核申报',     'API', NULL, NULL, '/api/v1/declarations/*/review', 'POST', 28, 'ACTIVE'),
(29, 'statistics:read',       '查看统计',     'API', NULL, NULL, '/api/v1/statistics', 'GET', 29, 'ACTIVE'),
(30, 'search:read',           '综合查询',     'API', NULL, NULL, '/api/v1/search', 'GET', 30, 'ACTIVE'),
(31, 'audit:read',            '查看操作日志', 'API', NULL, NULL, '/api/v1/audit-logs', 'GET', 31, 'ACTIVE');

-- ADMIN：全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;
-- CLERK
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2,1),(2,4),(2,5),(2,6),(2,7),(2,9),(2,10),
(2,16),(2,18),(2,20),(2,22),(2,23),(2,24),(2,25),(2,26),(2,29),(2,30);
-- REVIEWER
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3,1),(3,4),(3,5),(3,6),(3,7),(3,8),(3,9),(3,10),
(3,16),(3,18),(3,20),(3,22),(3,26),(3,27),(3,28),(3,29),(3,30);
-- ANALYST
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(4,1),(4,4),(4,5),(4,7),(4,9),(4,10),
(4,16),(4,18),(4,22),(4,26),(4,29),(4,30);
-- VIEWER
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(5,1),(5,4),(5,5),(5,7),(5,10),
(5,16),(5,18),(5,22),(5,29),(5,30);

INSERT INTO sys_user (id, username, password_hash, display_name, status, remark) VALUES
(1, 'admin',    '$2b$10$ANRpwJ6QomBTpqFuxyYMqu.rNCXEBhb2TtYZavXJeFn375iDqMtNy', '系统管理员', 'ACTIVE', '演示账号'),
(2, 'clerk',    '$2b$10$ANRpwJ6QomBTpqFuxyYMqu.rNCXEBhb2TtYZavXJeFn375iDqMtNy', '经办人·林样例', 'ACTIVE', '演示账号'),
(3, 'reviewer', '$2b$10$ANRpwJ6QomBTpqFuxyYMqu.rNCXEBhb2TtYZavXJeFn375iDqMtNy', '审核人·周样例', 'ACTIVE', '演示账号'),
(4, 'analyst',  '$2b$10$ANRpwJ6QomBTpqFuxyYMqu.rNCXEBhb2TtYZavXJeFn375iDqMtNy', '统计员·陈样例', 'ACTIVE', '演示账号'),
(5, 'viewer',   '$2b$10$ANRpwJ6QomBTpqFuxyYMqu.rNCXEBhb2TtYZavXJeFn375iDqMtNy', '只读·吴样例', 'ACTIVE', '演示账号');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1,1),(2,2),(3,3),(4,4),(5,5);

INSERT INTO taxpayer (id, taxpayer_code, taxpayer_name, taxpayer_type, contact_name, contact_phone, registration_status, remark) VALUES
(1, 'TP20260001', '星海示例科技有限公司',   'ENTERPRISE', '张样例', '13800000001', 'ACTIVE',   '虚构企业，仅用于教学演示'),
(2, 'TP20260002', '青禾模拟商贸有限公司',   'ENTERPRISE', '李样例', '13800000002', 'ACTIVE',   '虚构企业，仅用于教学演示'),
(3, 'TP20260003', '云岭虚构服务工作室',     'INDIVIDUAL', '王样例', '13800000003', 'ACTIVE',   '虚构个人主体'),
(4, 'TP20260004', '北辰示例咨询合伙企业',   'OTHER',      '赵样例', '13800000004', 'ACTIVE',   '虚构合伙企业'),
(5, 'TP20260005', '南桥模拟制造有限公司',   'ENTERPRISE', '孙样例', '13800000005', 'INACTIVE', '已停用的虚构企业'),
(6, 'TP20260006', '木棉虚构个人纳税人',     'INDIVIDUAL', '钱样例', '13800000006', 'ACTIVE',   '虚构个人主体');

INSERT INTO declaration_type (id, type_code, type_name, description, enabled) VALUES
(1, 'MOCK_COMPREHENSIVE', '模拟综合申报', '教学用综合申报类型，不代表真实税法分类', 1),
(2, 'MOCK_BUSINESS',      '模拟经营申报', '教学用经营申报类型，不代表真实税法分类', 1),
(3, 'MOCK_INDIVIDUAL',    '模拟个人申报', '教学用个人申报类型，不代表真实税法分类', 1);

INSERT INTO form_template (id, template_code, template_name, declaration_type_id, version_no, status, created_by, published_at) VALUES
(1, 'FT-COMP-01', '模拟综合申报表', 1, 1, 'PUBLISHED', 1, '2026-06-01 09:00:00'),
(2, 'FT-BIZ-01',  '模拟经营申报表', 2, 1, 'PUBLISHED', 1, '2026-06-01 09:10:00'),
(3, 'FT-IND-01',  '模拟个人申报表', 3, 1, 'PUBLISHED', 1, '2026-06-01 09:20:00');

INSERT INTO form_field (id, template_id, field_code, field_label, field_type, is_required, min_value, max_value, max_length, options_json, help_text, sort_order, enabled) VALUES
(1, 1, 'period_note',       '申报说明',             'TEXT',    1, NULL, NULL, 200, NULL, '简要说明本期模拟业务情况', 1, 1),
(2, 1, 'income_amount',     '模拟收入金额（元）',   'DECIMAL', 1, 0, 999999999.99, NULL, NULL, '仅用于教学统计，不代表真实税额', 2, 1),
(3, 1, 'employee_count',    '从业人数',             'INTEGER', 0, 0, 100000, NULL, NULL, NULL, 3, 1),
(4, 1, 'has_branch',        '是否设有分支机构',     'BOOLEAN', 0, NULL, NULL, NULL, NULL, NULL, 4, 1),
(5, 1, 'business_category', '模拟业务类别',         'RADIO',   1, NULL, NULL, NULL, '[{"label":"服务业","value":"SERVICE"},{"label":"商贸零售","value":"RETAIL"},{"label":"其他模拟类别","value":"OTHER"}]', NULL, 5, 1),
(6, 2, 'revenue_amount',    '模拟营业收入（元）',   'DECIMAL', 1, 0, 999999999.99, NULL, NULL, '虚构口径', 1, 1),
(7, 2, 'cost_amount',       '模拟成本金额（元）',   'DECIMAL', 1, 0, 999999999.99, NULL, NULL, '虚构口径', 2, 1),
(8, 2, 'business_scope',    '模拟经营范围说明',     'TEXT',    1, NULL, NULL, 500, NULL, NULL, 3, 1),
(9, 2, 'is_small_scale',    '是否模拟小规模主体',   'BOOLEAN', 0, NULL, NULL, NULL, NULL, NULL, 4, 1),
(10,2, 'start_date',        '经营起始日期',         'DATE',    1, NULL, NULL, NULL, NULL, NULL, 5, 1),
(11,3, 'personal_income',   '模拟个人收入（元）',   'DECIMAL', 1, 0, 999999999.99, NULL, NULL, '虚构口径', 1, 1),
(12,3, 'special_deduction', '模拟专项扣除（元）',   'DECIMAL', 0, 0, 999999999.99, NULL, NULL, '虚构口径', 2, 1),
(13,3, 'income_source',     '模拟收入来源',         'RADIO',   1, NULL, NULL, NULL, '[{"label":"劳务报酬","value":"LABOR"},{"label":"经营所得","value":"BUSINESS"},{"label":"其他模拟来源","value":"OTHER"}]', NULL, 3, 1),
(14,3, 'remark',            '补充说明',             'TEXT',    0, NULL, NULL, 300, NULL, NULL, 4, 1),
(15,3, 'is_resident',       '是否模拟居民个人',     'BOOLEAN', 0, NULL, NULL, NULL, NULL, NULL, 5, 1);

UPDATE declaration_type SET default_template_id = 1 WHERE id = 1;
UPDATE declaration_type SET default_template_id = 2 WHERE id = 2;
UPDATE declaration_type SET default_template_id = 3 WHERE id = 3;

INSERT INTO tax_declaration (id, declaration_no, taxpayer_id, taxpayer_name_snapshot, declaration_type_id, declaration_type_snapshot, template_id, template_version_no, period_start, period_end, status, declared_amount, form_data, created_by, submitted_at, last_reviewed_at, version, created_at, updated_at) VALUES
(1, 'DEC-2026-000001', 1, '星海示例科技有限公司', 1, '模拟综合申报', 1, 1, '2026-09-01', '2026-09-30', 'PENDING_REVIEW', 128000.00,
 '{"period_note":"2026年9月模拟申报","income_amount":128000.00,"employee_count":18,"has_branch":false,"business_category":"SERVICE"}', 2, '2026-10-20 10:00:00', NULL, 1, '2026-10-20 09:50:00', '2026-10-20 10:00:00'),
(2, 'DEC-2026-000002', 2, '青禾模拟商贸有限公司', 1, '模拟综合申报', 1, 1, '2026-09-01', '2026-09-30', 'PENDING_REVIEW', 96500.50,
 '{"period_note":"商贸模拟申报","income_amount":96500.50,"employee_count":9,"has_branch":true,"business_category":"RETAIL"}', 2, '2026-10-21 14:30:00', NULL, 1, '2026-10-21 14:10:00', '2026-10-21 14:30:00'),
(3, 'DEC-2026-000003', 3, '云岭虚构服务工作室', 3, '模拟个人申报', 3, 1, '2026-08-01', '2026-08-31', 'APPROVED', 42000.00,
 '{"personal_income":42000.00,"special_deduction":3000.00,"income_source":"LABOR","remark":"模拟劳务收入","is_resident":true}', 2, '2026-09-15 09:20:00', '2026-09-16 11:00:00', 2, '2026-09-15 09:00:00', '2026-09-16 11:00:00'),
(4, 'DEC-2026-000004', 1, '星海示例科技有限公司', 2, '模拟经营申报', 2, 1, '2026-07-01', '2026-07-31', 'APPROVED', 210000.00,
 '{"revenue_amount":210000.00,"cost_amount":160000.00,"business_scope":"模拟技术咨询服务","is_small_scale":false,"start_date":"2024-01-01"}', 2, '2026-08-10 10:00:00', '2026-08-11 09:30:00', 2, '2026-08-10 09:40:00', '2026-08-11 09:30:00'),
(5, 'DEC-2026-000005', 4, '北辰示例咨询合伙企业', 1, '模拟综合申报', 1, 1, '2026-09-01', '2026-09-30', 'RETURNED', 38000.00,
 '{"period_note":"合伙模拟申报","income_amount":38000.00,"employee_count":3,"has_branch":false,"business_category":"SERVICE"}', 2, '2026-10-22 16:20:00', '2026-10-23 10:10:00', 2, '2026-10-22 16:00:00', '2026-10-23 10:10:00'),
(6, 'DEC-2026-000006', 2, '青禾模拟商贸有限公司', 2, '模拟经营申报', 2, 1, '2026-09-01', '2026-09-30', 'DRAFT', 0.00,
 '{"revenue_amount":0,"cost_amount":0,"business_scope":"","is_small_scale":true,"start_date":"2025-03-01"}', 2, NULL, NULL, 0, '2026-10-25 15:00:00', '2026-10-25 15:00:00'),
(7, 'DEC-2026-000007', 5, '南桥模拟制造有限公司', 1, '模拟综合申报', 1, 1, '2026-06-01', '2026-06-30', 'WITHDRAWN', 150000.00,
 '{"period_note":"已撤回的模拟申报","income_amount":150000.00,"employee_count":25,"has_branch":false,"business_category":"OTHER"}', 2, '2026-07-05 09:00:00', NULL, 2, '2026-07-04 17:00:00', '2026-07-05 09:05:00'),
(8, 'DEC-2026-000008', 6, '木棉虚构个人纳税人', 3, '模拟个人申报', 3, 1, '2026-09-01', '2026-09-30', 'APPROVED', 31000.00,
 '{"personal_income":31000.00,"special_deduction":1500.00,"income_source":"BUSINESS","remark":"模拟个体经营","is_resident":true}', 2, '2026-10-01 10:00:00', '2026-10-02 09:00:00', 2, '2026-10-01 09:30:00', '2026-10-02 09:00:00');

INSERT INTO declaration_review (id, declaration_id, reviewer_id, review_action, review_comment, from_status, to_status, reviewed_at) VALUES
(1, 3, 3, 'APPROVE', '材料完整，模拟审核通过。', 'PENDING_REVIEW', 'APPROVED', '2026-09-16 11:00:00'),
(2, 4, 3, 'APPROVE', '模拟审核通过。', 'PENDING_REVIEW', 'APPROVED', '2026-08-11 09:30:00'),
(3, 5, 3, 'RETURN', '模拟收入说明缺少依据，请补充后重新提交。', 'PENDING_REVIEW', 'RETURNED', '2026-10-23 10:10:00'),
(4, 8, 3, 'APPROVE', '模拟审核通过。', 'PENDING_REVIEW', 'APPROVED', '2026-10-02 09:00:00');

INSERT INTO sys_operation_log (id, operator_id, operator_name_snapshot, operation_type, target_type, target_id, result, summary, trace_id, created_at) VALUES
(1, 2, '经办人·林样例', 'DECLARATION_SUBMIT', 'DECLARATION', '1', 'SUCCESS', '提交模拟申报 DEC-2026-000001', 'seed-trace-0001', '2026-10-20 10:00:00'),
(2, 3, '审核人·周样例', 'DECLARATION_REVIEW', 'DECLARATION', '5', 'SUCCESS', '退回模拟申报 DEC-2026-000005', 'seed-trace-0002', '2026-10-23 10:10:00'),
(3, 3, '审核人·周样例', 'DECLARATION_REVIEW', 'DECLARATION', '8', 'SUCCESS', '审核通过模拟申报 DEC-2026-000008', 'seed-trace-0003', '2026-10-02 09:00:00');

INSERT INTO sys_dict (dict_type, dict_code, dict_label, sort_order) VALUES
('DECLARATION_STATUS', 'DRAFT', '草稿', 1),
('DECLARATION_STATUS', 'PENDING_REVIEW', '待审核', 2),
('DECLARATION_STATUS', 'RETURNED', '已退回', 3),
('DECLARATION_STATUS', 'APPROVED', '审核通过', 4),
('DECLARATION_STATUS', 'WITHDRAWN', '已撤回', 5),
('TAXPAYER_TYPE', 'ENTERPRISE', '企业', 1),
('TAXPAYER_TYPE', 'INDIVIDUAL', '个人', 2),
('TAXPAYER_TYPE', 'OTHER', '其他模拟类型', 3);
