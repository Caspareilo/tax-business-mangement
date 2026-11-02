-- H2 (MODE=MySQL) test schema. Mirrors database/schema.sql with H2-compatible types.
-- Not used in production. JSON columns become TEXT so JDBC string binding works unchanged.

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

CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP    NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_username UNIQUE (username)
);
CREATE INDEX idx_user_status ON sys_user (status, deleted);

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(40)  NOT NULL,
    role_name   VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_code UNIQUE (role_code)
);

CREATE TABLE sys_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    group_name      VARCHAR(50)  NOT NULL DEFAULT '',
    permission_type VARCHAR(20)  NOT NULL DEFAULT 'ACTION',
    parent_id       BIGINT       NULL,
    url_pattern     VARCHAR(200) NULL,
    http_method     VARCHAR(10)  NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uk_permission_code UNIQUE (permission_code),
    CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES sys_permission (id)
);
CREATE INDEX idx_permission_parent ON sys_permission (parent_id);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
);
CREATE INDEX idx_user_role_role ON sys_user_role (role_id);

CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id)
        REFERENCES sys_permission (id) ON DELETE CASCADE
);
CREATE INDEX idx_role_permission_permission ON sys_role_permission (permission_id);

CREATE TABLE taxpayer (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    taxpayer_code       VARCHAR(40)  NOT NULL,
    taxpayer_name       VARCHAR(200) NOT NULL,
    taxpayer_type       VARCHAR(20)  NOT NULL,
    contact_name        VARCHAR(100) NOT NULL DEFAULT '',
    contact_phone       VARCHAR(40)  NOT NULL DEFAULT '',
    registration_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    remark              VARCHAR(500) NOT NULL DEFAULT '',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_taxpayer_code UNIQUE (taxpayer_code)
);
CREATE INDEX idx_taxpayer_type_status ON taxpayer (taxpayer_type, registration_status, deleted);

CREATE TABLE declaration_type (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type_code    VARCHAR(40)  NOT NULL,
    type_name    VARCHAR(100) NOT NULL,
    description  VARCHAR(500) NOT NULL DEFAULT '',
    enabled      TINYINT      NOT NULL DEFAULT 1,
    deadline_day INT          NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_declaration_type_code UNIQUE (type_code)
);

CREATE TABLE form_template (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_code       VARCHAR(40)  NOT NULL,
    template_name       VARCHAR(100) NOT NULL,
    declaration_type_id BIGINT       NOT NULL,
    version_no          INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by          BIGINT       NULL,
    published_at        TIMESTAMP    NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_template_code_version UNIQUE (template_code, version_no),
    CONSTRAINT fk_template_type FOREIGN KEY (declaration_type_id) REFERENCES declaration_type (id),
    CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
);
CREATE INDEX idx_template_type_status ON form_template (declaration_type_id, status);

CREATE TABLE form_field (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_id  BIGINT        NOT NULL,
    field_code   VARCHAR(50)   NOT NULL,
    field_label  VARCHAR(100)  NOT NULL,
    field_type   VARCHAR(20)   NOT NULL,
    required     TINYINT       NOT NULL DEFAULT 0,
    min_value    DECIMAL(18,2) NULL,
    max_value    DECIMAL(18,2) NULL,
    max_length   INT           NULL,
    options_json VARCHAR(2000) NULL,
    help_text    VARCHAR(255)  NOT NULL DEFAULT '',
    sort_order   INT           NOT NULL DEFAULT 0,
    enabled      TINYINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_field_template_code UNIQUE (template_id, field_code),
    CONSTRAINT fk_field_template FOREIGN KEY (template_id) REFERENCES form_template (id)
        ON DELETE CASCADE
);

CREATE TABLE tax_declaration (
    id                        BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    declaration_no            VARCHAR(50)   NOT NULL,
    taxpayer_id               BIGINT        NOT NULL,
    taxpayer_name_snapshot    VARCHAR(200)  NOT NULL,
    declaration_type_id       BIGINT        NOT NULL,
    declaration_type_snapshot VARCHAR(100)  NOT NULL,
    template_id               BIGINT        NOT NULL,
    template_version_no       INT           NOT NULL,
    period_start              DATE          NOT NULL,
    period_end                DATE          NOT NULL,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    declared_amount           DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    form_data_json            TEXT          NULL,
    created_by                BIGINT        NOT NULL,
    submitted_at              TIMESTAMP     NULL,
    last_reviewed_at          TIMESTAMP     NULL,
    version                   INT           NOT NULL DEFAULT 0,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                   TINYINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_declaration_no UNIQUE (declaration_no),
    CONSTRAINT fk_declaration_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayer (id),
    CONSTRAINT fk_declaration_type FOREIGN KEY (declaration_type_id) REFERENCES declaration_type (id),
    CONSTRAINT fk_declaration_template FOREIGN KEY (template_id) REFERENCES form_template (id),
    CONSTRAINT fk_declaration_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
);
CREATE INDEX idx_declaration_taxpayer_period
    ON tax_declaration (taxpayer_id, declaration_type_id, period_start, period_end);
CREATE INDEX idx_declaration_status ON tax_declaration (status, deleted);
CREATE INDEX idx_declaration_creator ON tax_declaration (created_by, deleted);

CREATE TABLE declaration_review (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    declaration_id BIGINT        NOT NULL,
    reviewer_id    BIGINT        NOT NULL,
    review_action  VARCHAR(20)   NOT NULL,
    review_comment VARCHAR(1000) NULL,
    from_status    VARCHAR(20)   NOT NULL,
    to_status      VARCHAR(20)   NOT NULL,
    reviewed_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_declaration FOREIGN KEY (declaration_id)
        REFERENCES tax_declaration (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (id)
);
CREATE INDEX idx_review_declaration ON declaration_review (declaration_id, reviewed_at);

CREATE TABLE sys_operation_log (
    id                     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    operator_id            BIGINT       NULL,
    operator_name_snapshot VARCHAR(100) NULL,
    operation_type         VARCHAR(50)  NOT NULL,
    target_type            VARCHAR(50)  NULL,
    target_id              VARCHAR(50)  NULL,
    result                 VARCHAR(20)  NOT NULL,
    summary                VARCHAR(500) NULL,
    trace_id               VARCHAR(100) NULL,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);
CREATE INDEX idx_log_created ON sys_operation_log (created_at);
CREATE INDEX idx_log_type ON sys_operation_log (operation_type);

CREATE TABLE sys_dict (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dict_type  VARCHAR(50)  NOT NULL,
    dict_key   VARCHAR(50)  NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    enabled    TINYINT      NOT NULL DEFAULT 1,
    CONSTRAINT uk_dict_type_key UNIQUE (dict_type, dict_key)
);
