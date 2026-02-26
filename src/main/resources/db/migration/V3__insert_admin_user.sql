-- Пароль: admin123 (BCrypt хеш)
INSERT INTO users (email, password, role)
VALUES ('admin@bank.com',
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
        'ROLE_ADMIN');
