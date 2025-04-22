-- 初始化测试数据库表和数据
-- 这个脚本创建了多个包含敏感数据的表，每个表约100条数据，用于测试不同的脱敏规则

-- 删除已存在的表（如果存在）
-- 注意：必须先删除有外键引用的表，再删除被引用的表
DROP TABLE IF EXISTS online_transactions;
DROP TABLE IF EXISTS medical_records;
DROP TABLE IF EXISTS financial_records;
DROP TABLE IF EXISTS employee_data;
DROP TABLE IF EXISTS customer_info;

-- 1. 客户信息表（包含个人身份信息）
CREATE TABLE customer_info (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10),
    age INT,
    id_card VARCHAR(18),  -- 身份证号
    phone VARCHAR(20),    -- 手机号
    email VARCHAR(50),    -- 邮箱
    address VARCHAR(200), -- 地址
    birth_date DATE,      -- 出生日期
    registration_date DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. 金融记录表（包含银行和财务信息）
CREATE TABLE financial_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    account_number VARCHAR(20),  -- 银行账号
    card_number VARCHAR(19),     -- 银行卡号
    card_cvv VARCHAR(4),         -- 信用卡CVV
    card_expiry VARCHAR(7),      -- 信用卡有效期
    balance DECIMAL(12, 2),      -- 账户余额
    income DECIMAL(12, 2),       -- 收入
    transaction_date DATETIME,   -- 交易日期
    transaction_type VARCHAR(20), -- 交易类型
    FOREIGN KEY (customer_id) REFERENCES customer_info(id)
);

-- 3. 医疗记录表（包含健康和医疗信息）
CREATE TABLE medical_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT,
    blood_type VARCHAR(5),       -- 血型
    height DECIMAL(5, 2),        -- 身高
    weight DECIMAL(5, 2),        -- 体重
    medical_history TEXT,        -- 病史
    diagnosis VARCHAR(200),      -- 诊断
    medication VARCHAR(200),     -- 药物
    doctor_notes TEXT,           -- 医生笔记
    visit_date DATE,             -- 就诊日期
    FOREIGN KEY (patient_id) REFERENCES customer_info(id)
);

-- 4. 员工数据表（包含工作和薪资信息）
CREATE TABLE employee_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(20),     -- 员工ID
    name VARCHAR(50),            -- 姓名
    department VARCHAR(50),      -- 部门
    position VARCHAR(50),        -- 职位
    salary DECIMAL(12, 2),       -- 薪资
    bonus DECIMAL(12, 2),        -- 奖金
    bank_account VARCHAR(20),    -- 银行账户
    social_security VARCHAR(20), -- 社保号
    hire_date DATE,              -- 入职日期
    performance_rating DECIMAL(3, 1) -- 绩效评分
);

-- 5. 在线交易表（包含电子商务和支付信息）
CREATE TABLE online_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    order_id VARCHAR(20),        -- 订单ID
    product_name VARCHAR(100),   -- 产品名称
    quantity INT,                -- 数量
    price DECIMAL(10, 2),        -- 价格
    payment_method VARCHAR(20),  -- 支付方式
    card_last_four VARCHAR(4),   -- 卡号后四位
    shipping_address VARCHAR(200), -- 收货地址
    ip_address VARCHAR(15),      -- IP地址
    transaction_date DATETIME,   -- 交易日期
    FOREIGN KEY (user_id) REFERENCES customer_info(id)
);

-- 插入客户信息表数据
INSERT INTO customer_info (name, gender, age, id_card, phone, email, address, birth_date) VALUES
('张三', '男', 28, '110101199301010011', '13800138001', 'zhangsan@example.com', '北京市海淀区中关村大街1号', '1993-01-01'),
('李四', '女', 35, '110101198601020022', '13900139002', 'lisi@example.com', '北京市朝阳区建国路2号', '1986-01-02'),
('王五', '男', 42, '110101197901030033', '13700137003', 'wangwu@example.com', '上海市浦东新区陆家嘴3号', '1979-01-03'),
('赵六', '女', 31, '110101199001040044', '13600136004', 'zhaoliu@example.com', '广州市天河区体育西路4号', '1990-01-04'),
('钱七', '男', 25, '110101199601050055', '13500135005', 'qianqi@example.com', '深圳市南山区科技园5号', '1996-01-05'),
('孙八', '女', 38, '110101198301060066', '13400134006', 'sunba@example.com', '成都市武侯区人民南路6号', '1983-01-06'),
('周九', '男', 45, '110101197601070077', '13300133007', 'zhoujiu@example.com', '重庆市渝中区解放碑7号', '1976-01-07'),
('吴十', '女', 29, '110101199201080088', '13200132008', 'wushi@example.com', '武汉市江汉区汉口路8号', '1992-01-08'),
('郑十一', '男', 33, '110101198801090099', '13100131009', 'zhengshiyi@example.com', '南京市鼓楼区中山路9号', '1988-01-09'),
('王十二', '女', 27, '110101199401100100', '13000130010', 'wangshier@example.com', '杭州市西湖区西湖路10号', '1994-01-10');

-- 继续插入更多客户数据...
-- 为了简化，这里只展示了10条记录，实际应该插入约100条

-- 批量生成90条额外的客户数据
INSERT INTO customer_info (name, gender, age, id_card, phone, email, address, birth_date)
SELECT 
    CONCAT('用户', id + 10),
    CASE WHEN id % 2 = 0 THEN '男' ELSE '女' END,
    20 + (id % 50),
    CONCAT('1101011990', LPAD(id + 10, 8, '0')),
    CONCAT('139', LPAD(id + 1000000, 8, '0')),
    CONCAT('user', id + 10, '@example.com'),
    CONCAT('地址示例 ', id + 10, ' 号'),
    DATE_ADD('1970-01-01', INTERVAL id * 100 DAY)
FROM customer_info
WHERE id <= 90;

-- 插入金融记录表数据
INSERT INTO financial_records (customer_id, account_number, card_number, card_cvv, card_expiry, balance, income, transaction_date, transaction_type)
SELECT 
    id,
    CONCAT('622848', LPAD(id, 10, '0')),
    CONCAT('6225', LPAD(id * 3, 12, '0')),
    LPAD(id % 999, 3, '0'),
    CONCAT('20', LPAD(25 + (id % 10), 2, '0'), '/', LPAD(1 + (id % 12), 2, '0')),
    10000.00 + (id * 1000.00),
    5000.00 + (id * 500.00),
    DATE_ADD(CURRENT_DATE(), INTERVAL -id DAY),
    CASE id % 5
        WHEN 0 THEN '存款'
        WHEN 1 THEN '取款'
        WHEN 2 THEN '转账'
        WHEN 3 THEN '支付'
        ELSE '退款'
    END
FROM customer_info;

-- 插入医疗记录表数据
INSERT INTO medical_records (patient_id, blood_type, height, weight, medical_history, diagnosis, medication, doctor_notes, visit_date)
SELECT 
    id,
    CASE id % 8
        WHEN 0 THEN 'A+'
        WHEN 1 THEN 'A-'
        WHEN 2 THEN 'B+'
        WHEN 3 THEN 'B-'
        WHEN 4 THEN 'AB+'
        WHEN 5 THEN 'AB-'
        WHEN 6 THEN 'O+'
        ELSE 'O-'
    END,
    160.00 + (id % 40),
    50.00 + (id % 50),
    CASE id % 4
        WHEN 0 THEN '无重大病史'
        WHEN 1 THEN '高血压病史'
        WHEN 2 THEN '糖尿病病史'
        ELSE '心脏病史'
    END,
    CASE id % 6
        WHEN 0 THEN '健康检查'
        WHEN 1 THEN '上呼吸道感染'
        WHEN 2 THEN '胃炎'
        WHEN 3 THEN '高血压'
        WHEN 4 THEN '糖尿病'
        ELSE '关节炎'
    END,
    CASE id % 5
        WHEN 0 THEN '无'
        WHEN 1 THEN '阿莫西林'
        WHEN 2 THEN '布洛芬'
        WHEN 3 THEN '降压药'
        ELSE '胰岛素'
    END,
    CONCAT('患者状况', CASE id % 3
        WHEN 0 THEN '良好'
        WHEN 1 THEN '稳定'
        ELSE '需要随访'
    END),
    DATE_ADD(CURRENT_DATE(), INTERVAL -id * 7 DAY)
FROM customer_info;

-- 插入员工数据表数据
INSERT INTO employee_data (employee_id, name, department, position, salary, bonus, bank_account, social_security, hire_date, performance_rating)
SELECT 
    CONCAT('EMP', LPAD(id, 6, '0')),
    CONCAT('员工', id),
    CASE id % 5
        WHEN 0 THEN '技术部'
        WHEN 1 THEN '市场部'
        WHEN 2 THEN '财务部'
        WHEN 3 THEN '人力资源部'
        ELSE '销售部'
    END,
    CASE id % 4
        WHEN 0 THEN '初级'
        WHEN 1 THEN '中级'
        WHEN 2 THEN '高级'
        ELSE '经理'
    END,
    8000.00 + (id * 1000.00),
    1000.00 + (id * 500.00),
    CONCAT('6228', LPAD(id, 12, '0')),
    CONCAT('SS', LPAD(id, 10, '0')),
    DATE_ADD('2015-01-01', INTERVAL id * 30 DAY),
    3.0 + (id % 20) / 10.0
FROM customer_info
WHERE id <= 100;

-- 插入在线交易表数据
INSERT INTO online_transactions (user_id, order_id, product_name, quantity, price, payment_method, card_last_four, shipping_address, ip_address, transaction_date)
SELECT 
    id,
    CONCAT('ORD', LPAD(id * 2, 8, '0')),
    CASE id % 10
        WHEN 0 THEN '智能手机'
        WHEN 1 THEN '笔记本电脑'
        WHEN 2 THEN '平板电脑'
        WHEN 3 THEN '智能手表'
        WHEN 4 THEN '无线耳机'
        WHEN 5 THEN '智能音箱'
        WHEN 6 THEN '电视'
        WHEN 7 THEN '相机'
        WHEN 8 THEN '游戏机'
        ELSE '电子书阅读器'
    END,
    1 + (id % 5),
    999.00 + (id % 10) * 100.00,
    CASE id % 3
        WHEN 0 THEN '信用卡'
        WHEN 1 THEN '支付宝'
        ELSE '微信支付'
    END,
    LPAD(id % 9999, 4, '0'),
    CONCAT('收货地址 ', id, ' 号'),
    CONCAT('192.168.', id % 255, '.', id % 100),
    DATE_ADD(CURRENT_DATE(), INTERVAL -id DAY)
FROM customer_info;