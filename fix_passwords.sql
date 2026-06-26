UPDATE dashboard_users SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMye8JonxY8J2m8iQV9ZE.x4MR3fczYV8fO' WHERE id IN (1, 2);
SELECT id, username, password FROM dashboard_users;
