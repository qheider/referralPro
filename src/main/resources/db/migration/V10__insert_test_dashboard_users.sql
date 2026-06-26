-- Insert a test dashboard user for Company ID 1
-- Username: admin@company.com
-- Password: password123 (BCrypt hashed with strength 10)
INSERT INTO dashboard_users (company_id, username, password, role, created_at, updated_at)
VALUES (
    1,
    'admin@company.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye8JonxY8J2m8iQV9ZE.x4MR3fczYV8fO',
    'COMPANY_ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Insert a test dashboard user for Company ID 2
-- Username: admin2@company.com
-- Password: password123 (BCrypt hashed with strength 10)
INSERT INTO dashboard_users (company_id, username, password, role, created_at, updated_at)
VALUES (
    2,
    'admin2@company.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye8JonxY8J2m8iQV9ZE.x4MR3fczYV8fO',
    'COMPANY_ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
