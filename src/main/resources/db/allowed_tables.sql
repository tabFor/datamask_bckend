CREATE TABLE IF NOT EXISTS allowed_tables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 插入一些默认允许的表
INSERT INTO allowed_tables (table_name, description) VALUES
('audit_logs', '审计日志表'),
('users', '用户表'),
('masked_data', '脱敏数据表')
ON DUPLICATE KEY UPDATE is_active = TRUE; 