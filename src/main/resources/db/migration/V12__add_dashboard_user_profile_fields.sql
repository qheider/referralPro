ALTER TABLE dashboard_users
    ADD COLUMN first_name VARCHAR(100) NULL AFTER password,
    ADD COLUMN last_name VARCHAR(100) NULL AFTER first_name,
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD COLUMN last_login_at TIMESTAMP NULL AFTER status;

ALTER TABLE dashboard_users
    ADD INDEX idx_dashboard_users_role (role),
    ADD INDEX idx_dashboard_users_status (status);
