-- 脱敏规则表
CREATE TABLE IF NOT EXISTS desensitization_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    pattern VARCHAR(50) NOT NULL,
    prefix_length INT,
    suffix_length INT,
    replacement_char VARCHAR(10),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_rule_id (rule_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4; 