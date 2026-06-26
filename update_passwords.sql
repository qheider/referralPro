UPDATE dashboard_users SET password = '$2a$10$/UPERV.XPI7jEZaFPCOUYucDI7/o66We0zuRE19UIlQYI1bAWtL2i' WHERE id IN (1, 2);
SELECT id, username, password FROM dashboard_users;
