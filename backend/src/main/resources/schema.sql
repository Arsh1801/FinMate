-- ===================================================================
-- FinMate Database Schema - MySQL 8 / H2 MySQL Compatibility Mode
-- ===================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(60) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    phone VARCHAR(10) NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    opening_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency_code CHAR(3) NOT NULL DEFAULT 'INR',
    theme VARCHAR(5) NOT NULL DEFAULT 'LIGHT',
    page_size TINYINT NOT NULL DEFAULT 10,
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    default_alert_threshold TINYINT NOT NULL DEFAULT 80,
    failed_login_attempts TINYINT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    name VARCHAR(40) NOT NULL,
    type VARCHAR(7) NOT NULL,
    color CHAR(7) NULL,
    icon VARCHAR(30) NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_cat_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS recurring_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    type VARCHAR(7) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(100) NOT NULL,
    payment_method VARCHAR(15) NOT NULL,
    frequency VARCHAR(10) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    next_run_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_rec_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    type VARCHAR(7) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(100) NOT NULL,
    notes VARCHAR(500) NULL,
    payment_method VARCHAR(15) NOT NULL,
    recurring_rule_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_tx_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_tx_recurring FOREIGN KEY (recurring_rule_id) REFERENCES recurring_rules(id),
    INDEX idx_tx_user_date (user_id, transaction_date DESC, id DESC),
    INDEX idx_tx_user_type_date (user_id, type, transaction_date),
    INDEX idx_tx_user_cat_date (user_id, category_id, transaction_date)
);

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(30) NOT NULL,
    CONSTRAINT fk_tag_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_user_tag UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS transaction_tags (
    transaction_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (transaction_id, tag_id),
    CONSTRAINT fk_tt_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_tt_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    budget_year SMALLINT NOT NULL,
    budget_month TINYINT NOT NULL,
    limit_amount DECIMAL(15,2) NOT NULL,
    alert_threshold TINYINT NOT NULL DEFAULT 80,
    last_alert_level VARCHAR(10) NOT NULL DEFAULT 'NONE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_bgt_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bgt_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS savings_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    target_amount DECIMAL(15,2) NOT NULL,
    target_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    note VARCHAR(255) NULL,
    icon VARCHAR(30) NULL,
    created_date DATE NOT NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS goal_contributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    contribution_date DATE NOT NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_contrib_goal FOREIGN KEY (goal_id) REFERENCES savings_goals(id) ON DELETE CASCADE,
    INDEX idx_goal_contrib_date (goal_id, contribution_date DESC)
);

CREATE TABLE IF NOT EXISTS expense_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    created_by BIGINT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_grp_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME NOT NULL,
    left_at DATETIME NULL,
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_gm_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_group_member UNIQUE (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS group_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    invited_user_id BIGINT NOT NULL,
    invited_by BIGINT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    responded_at DATETIME NULL,
    CONSTRAINT fk_inv_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_inv_user FOREIGN KEY (invited_user_id) REFERENCES users(id),
    CONSTRAINT fk_inv_by FOREIGN KEY (invited_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS shared_expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    paid_by BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(100) NOT NULL,
    notes VARCHAR(500) NULL,
    expense_date DATE NOT NULL,
    category_id BIGINT NULL,
    split_type VARCHAR(10) NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    deleted_by BIGINT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_se_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_se_payer FOREIGN KEY (paid_by) REFERENCES users(id),
    CONSTRAINT fk_se_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_se_creator FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_se_updater FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_se_deleter FOREIGN KEY (deleted_by) REFERENCES users(id),
    INDEX idx_se_grp_date (group_id, deleted_at, expense_date DESC)
);

CREATE TABLE IF NOT EXISTS expense_splits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    share_amount DECIMAL(15,2) NOT NULL,
    percentage DECIMAL(5,2) NULL,
    CONSTRAINT fk_es_expense FOREIGN KEY (expense_id) REFERENCES shared_expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_es_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_expense_user UNIQUE (expense_id, user_id),
    INDEX idx_es_user (user_id)
);

CREATE TABLE IF NOT EXISTS settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    settlement_date DATE NOT NULL,
    note VARCHAR(255) NULL,
    recorded_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    updated_at DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stl_group FOREIGN KEY (group_id) REFERENCES expense_groups(id),
    CONSTRAINT fk_stl_from FOREIGN KEY (from_user_id) REFERENCES users(id),
    CONSTRAINT fk_stl_to FOREIGN KEY (to_user_id) REFERENCES users(id),
    CONSTRAINT fk_stl_rec FOREIGN KEY (recorded_by) REFERENCES users(id),
    INDEX idx_stl_grp_status (group_id, status, confirmed_at)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(255) NOT NULL,
    ref_type VARCHAR(30) NULL,
    ref_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    dedupe_key VARCHAR(120) NULL,
    emailed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_unread (user_id, is_read, created_at DESC)
);
