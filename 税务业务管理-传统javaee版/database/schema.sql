-- 税务业务管理系统（传统 Java EE 版）
-- MySQL 8.0 建库脚本。所有表使用 InnoDB + utf8mb4。
-- 执行方式：mysql -u <user> -p < database/schema.sql
-- 演示数据见 database/seed-demo.sql。

CREATE DATABASE IF NOT EXISTS tax_demo
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE tax_demo;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS declaration_review;
DROP TABLE IF EXISTS tax_declaration;
DROP TABLE IF EXISTS form_field;
DROP TABLE IF EXISTS form_template;
DROP TABLE IF EXISTS declaration_type;
DROP TABLE IF EXISTS taxpayer;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password_hash   VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要，禁止保存明文',
    display_name    VARCHAR(100) NOT NULL COMMENT '显示名称',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    last_login_at   DATETIME     NULL COMMENT '最近登录时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_status (status, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(40)  NOT NULL COMMENT '角色编码，如 ADMIN/CLERK',
    role_name   VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色';

CREATE TABLE sys_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码，如 declaration:create',
    permission_name VARCHAR(100) NOT NULL,
    group_name      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '权限分组，仅用于界面展示',
    permission_type VARCHAR(20)  NOT NULL DEFAULT 'ACTION' COMMENT 'MENU/ACTION/API',
    parent_id       BIGINT       NULL COMMENT '父权限，可为空',
    url_pattern     VARCHAR(200) NULL COMMENT '关联 URL 前缀，便于审计',
    http_method     VARCHAR(10)  NULL COMMENT 'GET/POST，可为空表示不限',
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_permission_parent (parent_id),
    CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id)
        REFERENCES sys_permission (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限点';

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permission_permission (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id)
        REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id)
        REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联';

CREATE TABLE taxpayer (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    taxpayer_code       VARCHAR(40)  NOT NULL COMMENT '纳税人识别号（演示用虚构编号）',
    taxpayer_name       VARCHAR(200) NOT NULL,
    taxpayer_type       VARCHAR(20)  NOT NULL COMMENT 'COMPANY/INDIVIDUAL/INSTITUTION',
    contact_name        VARCHAR(100) NOT NULL DEFAULT '',
    contact_phone       VARCHAR(40)  NOT NULL DEFAULT '',
    registration_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    remark              VARCHAR(500) NOT NULL DEFAULT '',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_taxpayer_code (taxpayer_code),
    KEY idx_taxpayer_name (taxpayer_name),
    KEY idx_taxpayer_type_status (taxpayer_type, registration_status, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '纳税人';

CREATE TABLE declaration_type (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    type_code    VARCHAR(40)  NOT NULL COMMENT '申报类型编码',
    type_name    VARCHAR(100) NOT NULL,
    description  VARCHAR(500) NOT NULL DEFAULT '',
    enabled      TINYINT(1)   NOT NULL DEFAULT 1,
    deadline_day INT          NULL COMMENT '每月申报截止日，可为空',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_declaration_type_code (type_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '申报类型';

CREATE TABLE form_template (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    template_code       VARCHAR(40) NOT NULL COMMENT '同一模板编码的多个版本共享该编码',
    template_name       VARCHAR(100) NOT NULL,
    declaration_type_id BIGINT      NOT NULL,
    version_no          INT         NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DISABLED',
    created_by          BIGINT      NULL,
    published_at        DATETIME    NULL,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code_version (template_code, version_no),
    KEY idx_template_type_status (declaration_type_id, status),
    KEY idx_template_created_by (created_by),
    CONSTRAINT fk_template_type FOREIGN KEY (declaration_type_id)
        REFERENCES declaration_type (id),
    CONSTRAINT fk_template_creator FOREIGN KEY (created_by)
        REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '表单模板（带版本号）';

CREATE TABLE form_field (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    template_id  BIGINT        NOT NULL,
    field_code   VARCHAR(50)   NOT NULL,
    field_label  VARCHAR(100)  NOT NULL,
    field_type   VARCHAR(20)   NOT NULL COMMENT 'TEXT/INTEGER/AMOUNT/DATE/RADIO/MULTI/BOOLEAN',
    required     TINYINT(1)    NOT NULL DEFAULT 0,
    min_value    DECIMAL(18,2) NULL,
    max_value    DECIMAL(18,2) NULL,
    max_length   INT           NULL,
    options_json JSON          NULL COMMENT '单选/多选选项，JSON 数组',
    help_text    VARCHAR(255)  NOT NULL DEFAULT '',
    sort_order   INT           NOT NULL DEFAULT 0,
    enabled      TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_field_template_code (template_id, field_code),
    CONSTRAINT fk_field_template FOREIGN KEY (template_id)
        REFERENCES form_template (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '模板字段定义';

CREATE TABLE tax_declaration (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    declaration_no            VARCHAR(50)   NOT NULL COMMENT '申报编号',
    taxpayer_id               BIGINT        NOT NULL,
    taxpayer_name_snapshot    VARCHAR(200)  NOT NULL COMMENT '提交时的纳税人名称快照',
    declaration_type_id       BIGINT        NOT NULL,
    declaration_type_snapshot VARCHAR(100)  NOT NULL COMMENT '提交时的申报类型名称快照',
    template_id               BIGINT        NOT NULL,
    template_version_no       INT           NOT NULL COMMENT '提交时模板版本快照',
    period_start              DATE          NOT NULL,
    period_end                DATE          NOT NULL,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
        COMMENT 'DRAFT/PENDING_REVIEW/RETURNED/APPROVED/WITHDRAWN',
    declared_amount           DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    form_data_json            JSON          NULL COMMENT '动态表单数据，键为 field_code',
    created_by                BIGINT        NOT NULL,
    submitted_at              DATETIME      NULL,
    last_reviewed_at          DATETIME      NULL,
    version                   INT           NOT NULL DEFAULT 0 COMMENT '乐观锁/变更序号',
    created_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                   TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_declaration_no (declaration_no),
    KEY idx_declaration_taxpayer_period (taxpayer_id, declaration_type_id, period_start, period_end),
    KEY idx_declaration_status (status, deleted),
    KEY idx_declaration_creator (created_by, deleted),
    KEY idx_declaration_submitted (submitted_at),
    CONSTRAINT fk_declaration_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayer (id),
    CONSTRAINT fk_declaration_type FOREIGN KEY (declaration_type_id) REFERENCES declaration_type (id),
    CONSTRAINT fk_declaration_template FOREIGN KEY (template_id) REFERENCES form_template (id),
    CONSTRAINT fk_declaration_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '申报单';

CREATE TABLE declaration_review (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    declaration_id BIGINT        NOT NULL,
    reviewer_id    BIGINT        NOT NULL,
    review_action  VARCHAR(20)   NOT NULL COMMENT 'APPROVE/RETURN',
    review_comment VARCHAR(1000) NULL,
    from_status    VARCHAR(20)   NOT NULL,
    to_status      VARCHAR(20)   NOT NULL,
    reviewed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_declaration (declaration_id, reviewed_at),
    KEY idx_review_reviewer (reviewer_id),
    CONSTRAINT fk_review_declaration FOREIGN KEY (declaration_id)
        REFERENCES tax_declaration (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '审核记录';

CREATE TABLE sys_operation_log (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    operator_id            BIGINT       NULL,
    operator_name_snapshot VARCHAR(100) NULL COMMENT '操作人名称快照，登录失败时可为空',
    operation_type         VARCHAR(50)  NOT NULL,
    target_type            VARCHAR(50)  NULL,
    target_id              VARCHAR(50)  NULL,
    result                 VARCHAR(20)  NOT NULL COMMENT 'SUCCESS/FAILURE',
    summary                VARCHAR(500) NULL COMMENT '审计摘要，禁止写入密码/令牌/完整敏感表单',
    trace_id               VARCHAR(100) NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_created (created_at),
    KEY idx_log_type (operation_type),
    KEY idx_log_operator (operator_id),
    CONSTRAINT fk_log_operator FOREIGN KEY (operator_id)
        REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作审计日志';

CREATE TABLE sys_dict (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    dict_type  VARCHAR(50)  NOT NULL,
    dict_key   VARCHAR(50)  NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    enabled    TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_key (dict_type, dict_key),
    KEY idx_dict_type (dict_type, enabled, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统字典';
