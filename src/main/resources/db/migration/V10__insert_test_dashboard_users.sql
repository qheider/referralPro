-- Insert test companies (ignore if they already exist)
INSERT IGNORE INTO companies (name, email, status, api_key, created_at, updated_at)
VALUES (
    'Test Company 1',
    'company1@test.com',
    'ACTIVE',
    'test_api_key_company_1_abc123xyz789',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT IGNORE INTO companies (name, email, status, api_key, created_at, updated_at)
VALUES (
    'Test Company 2',
    'company2@test.com',
    'ACTIVE',
    'test_api_key_company_2_def456uvw012',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert test dashboard users for Company ID 1 (ignore if they already exist)
-- Username: admin@company.com
-- Password: password123 (BCrypt hashed with strength 10)
INSERT IGNORE INTO dashboard_users (company_id, username, password, role, created_at, updated_at)
VALUES (
    1,
    'admin@company.com',
    '$2a$10$Hj78qmYX3AJFRWMxFlbbP.6e6JtvLQACx6zQon1.lxPLwzLXQQs2i',
    'COMPANY_ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert test dashboard user for Company ID 2 (ignore if already exists)
-- Username: admin2@company.com
-- Password: password123 (BCrypt hashed with strength 10)
INSERT IGNORE INTO dashboard_users (company_id, username, password, role, created_at, updated_at)
VALUES (
    2,
    'admin2@company.com',
    '$2a$10$Hj78qmYX3AJFRWMxFlbbP.6e6JtvLQACx6zQon1.lxPLwzLXQQs2i',
    'COMPANY_ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);