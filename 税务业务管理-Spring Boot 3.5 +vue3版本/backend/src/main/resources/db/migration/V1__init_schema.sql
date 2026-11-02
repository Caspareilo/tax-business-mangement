-- 税务业务管理系统 V1.0 初始化表结构（教学模拟）
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    remark        VARCHAR(255) NULL,
    last_login_at DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_code   VARCHAR(64)  NOT NULL,
    role_name   VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    permission_type VARCHAR(20)  NOT NULL,
    parent_id       BIGINT       NULL,
    route_path      VARCHAR(255) NULL,
    api_pattern     VARCHAR(255) NULL,
    http_method     VARCHAR(12)  NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    KEY idx_sys_role_permission_permission (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE taxpayer (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    taxpayer_code       VARCHAR(64)  NOT NULL,
    taxpayer_name       VARCHAR(200) NOT NULL,
    taxpayer_type       VARCHAR(30)  NOT NULL,
    contact_name        VARCHAR(100) NULL,
    contact_phone       VARCHAR(40)  NULL,
    registration_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    remark              VARCHAR(500) NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_taxpayer_code (taxpayer_code),
    KEY idx_taxpayer_name (taxpayer_name),
    KEY idx_taxpayer_type_status (taxpayer_type, registration_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE declaration_type (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    type_code           VARCHAR(64)  NOT NULL,
    type_name           VARCHAR(100) NOT NULL,
    description         VARCHAR(500) NULL,
    enabled             TINYINT      NOT NULL DEFAULT 1,
    default_template_id BIGINT       NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_declaration_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE form_template (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    template_code       VARCHAR(64)  NOT NULL,
    template_name       VARCHAR(150) NOT NULL,
    declaration_type_id BIGINT       NOT NULL,
    version_no          INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by          BIGINT       NOT NULL,
    published_at        DATETIME     NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_form_template_code_version (template_code, version_no),
    KEY idx_form_template_type (declaration_type_id, status),
    CONSTRAINT fk_form_template_type FOREIGN KEY (declaration_type_id) REFERENCES declaration_type (id),
    CONSTRAINT fk_form_template_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE form_field (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    template_id     BIGINT        NOT NULL,
    field_code      VARCHAR(64)   NOT NULL,
    field_label     VARCHAR(150)  NOT NULL,
    field_type      VARCHAR(30)   NOT NULL,
    is_required     TINYINT       NOT NULL DEFAULT 0,
    min_value       DECIMAL(18,2) NULL,
    max_value       DECIMAL(18,2) NULL,
    max_length      INT           NULL,
    options_json    JSON          NULL,
    validation_json JSON          NULL,
    help_text       VARCHAR(255)  NULL,
    sort_order      INT           NOT NULL DEFAULT 0,
    enabled         TINYINT       NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_form_field_code (template_id, field_code),
    CONSTRAINT fk_form_field_template FOREIGN KEY (template_id) REFERENCES form_template (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tax_declaration (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    declaration_no            VARCHAR(64)   NOT NULL,
    taxpayer_id               BIGINT        NOT NULL,
    taxpayer_name_snapshot    VARCHAR(200)  NOT NULL,
    declaration_type_id       BIGINT        NOT NULL,
    declaration_type_snapshot VARCHAR(100)  NOT NULL,
    template_id               BIGINT        NOT NULL,
    template_version_no       INT           NOT NULL,
    period_start              DATE          NOT NULL,
    period_end                DATE          NOT NULL,
    status                    VARCHAR(30)   NOT NULL,
    declared_amount           DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    form_data                 JSON          NOT NULL,
    created_by                BIGINT        NOT NULL,
    submitted_at              DATETIME      NULL,
    last_reviewed_at          DATETIME      NULL,
    version                   INT           NOT NULL DEFAULT 0,
    created_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                   TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tax_declaration_no (declaration_no),
    KEY idx_decl_taxpayer_period (taxpayer_id, declaration_type_id, period_start, period_end, status),
    KEY idx_decl_status_submitted (status, submitted_at),
    KEY idx_decl_created_by (created_by, created_at),
    CONSTRAINT fk_decl_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayer (id),
    CONSTRAINT fk_decl_type FOREIGN KEY (declaration_type_id) REFERENCES declaration_type (id),
    CONSTRAINT fk_decl_template FOREIGN KEY (template_id) REFERENCES form_template (id),
    CONSTRAINT fk_decl_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE declaration_review (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    declaration_id BIGINT        NOT NULL,
    reviewer_id    BIGINT        NOT NULL,
    review_action  VARCHAR(20)   NOT NULL,
    review_comment VARCHAR(1000) NULL,
    from_status    VARCHAR(30)   NOT NULL,
    to_status      VARCHAR(30)   NOT NULL,
    reviewed_at    DATETIME      NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_declaration (declaration_id, reviewed_at),
    KEY idx_review_reviewer (reviewer_id, reviewed_at),
    CONSTRAINT fk_review_declaration FOREIGN KEY (declaration_id) REFERENCES tax_declaration (id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_operation_log (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    operator_id            BIGINT       NULL,
    operator_name_snapshot VARCHAR(100) NULL,
    operation_type         VARCHAR(80)  NOT NULL,
    target_type            VARCHAR(80)  NULL,
    target_id              VARCHAR(80)  NULL,
    result                 VARCHAR(20)  NOT NULL,
    summary                VARCHAR(500) NULL,
    trace_id               VARCHAR(100) NULL,
    ip_masked              VARCHAR(80)  NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_created (created_at),
    KEY idx_log_operator (operator_id, created_at),
    KEY idx_log_type (operation_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_dict (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    dict_type  VARCHAR(64)  NOT NULL,
    dict_code  VARCHAR(64)  NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    enabled    TINYINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_type_code (dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
