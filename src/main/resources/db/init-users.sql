-- 初始化用户数据

-- 删除现有的用户表（如果存在）
DROP TABLE IF EXISTS user;

-- 创建用户表（如果不存在）
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- 插入默认用户
-- 密码在实际应用中应该加密存储，这里仅作为示例
INSERT INTO user (username, password, role) VALUES
('admin', 'admin123', 'ADMIN'),                  -- 管理员
('analyst', 'analyst123', 'DATA_ANALYST'),      -- 数据分析师
('operator', 'operator123', 'DATA_OPERATOR');   -- 数据操作员 