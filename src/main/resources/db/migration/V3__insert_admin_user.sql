-- Пароль: admin123 (BCrypt хеш)
INSERT INTO users (email, password, role)
VALUES ('admin@bank.com',
        '$2a$10$CmqX.X09TZvzRxdku81P4.HU1QFGsfRQSDEnEiUx8/AcD7RFtJD72',
        'ROLE_ADMIN');
