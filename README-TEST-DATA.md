# 数据脱敏测试数据说明

本文档描述了用于测试数据脱敏功能的测试数据库表结构和数据。

## 测试数据表概述

系统创建了5个包含不同类型敏感数据的表，每个表约100条数据，用于测试不同的脱敏规则：

1. **客户信息表 (customer_info)**：包含个人身份信息
2. **金融记录表 (financial_records)**：包含银行和财务信息
3. **医疗记录表 (medical_records)**：包含健康和医疗信息
4. **员工数据表 (employee_data)**：包含工作和薪资信息
5. **在线交易表 (online_transactions)**：包含电子商务和支付信息

## 表结构和敏感字段

### 1. 客户信息表 (customer_info)

包含个人基本信息，适用于测试个人身份信息脱敏规则。

| 字段名 | 数据类型 | 敏感程度 | 适用脱敏方式 |
|-------|---------|---------|------------|
| id | INT | 低 | 不需脱敏 |
| name | VARCHAR(50) | 中 | 部分遮盖（如"张*"） |
| gender | VARCHAR(10) | 低 | 不需脱敏 |
| age | INT | 低 | 不需脱敏或范围化 |
| id_card | VARCHAR(18) | 高 | 部分遮盖（如"110101********0011"） |
| phone | VARCHAR(20) | 高 | 部分遮盖（如"138****8001"） |
| email | VARCHAR(50) | 中 | 部分遮盖（如"zh****@example.com"） |
| address | VARCHAR(200) | 高 | 部分遮盖或地址泛化 |
| birth_date | DATE | 中 | 日期泛化（如只保留年份） |

### 2. 金融记录表 (financial_records)

包含银行和财务信息，适用于测试金融数据脱敏规则。

| 字段名 | 数据类型 | 敏感程度 | 适用脱敏方式 |
|-------|---------|---------|------------|
| account_number | VARCHAR(20) | 高 | 部分遮盖（如"622848****0011"） |
| card_number | VARCHAR(19) | 高 | 部分遮盖（如"6225********1234"） |
| card_cvv | VARCHAR(4) | 高 | 完全遮盖（如"***"） |
| card_expiry | VARCHAR(7) | 高 | 完全遮盖（如"**/**"） |
| balance | DECIMAL(12, 2) | 高 | 数值范围化或随机化 |
| income | DECIMAL(12, 2) | 高 | 数值范围化或随机化 |
| transaction_type | VARCHAR(20) | 低 | 不需脱敏 |

### 3. 医疗记录表 (medical_records)

包含健康和医疗信息，适用于测试医疗数据脱敏规则。

| 字段名 | 数据类型 | 敏感程度 | 适用脱敏方式 |
|-------|---------|---------|------------|
| blood_type | VARCHAR(5) | 中 | 可能需要脱敏 |
| height | DECIMAL(5, 2) | 低 | 范围化或不需脱敏 |
| weight | DECIMAL(5, 2) | 中 | 范围化 |
| medical_history | TEXT | 高 | 关键词替换或分类泛化 |
| diagnosis | VARCHAR(200) | 高 | 关键词替换或分类泛化 |
| medication | VARCHAR(200) | 高 | 关键词替换或分类泛化 |
| doctor_notes | TEXT | 高 | 关键词替换或分类泛化 |

### 4. 员工数据表 (employee_data)

包含工作和薪资信息，适用于测试员工数据脱敏规则。

| 字段名 | 数据类型 | 敏感程度 | 适用脱敏方式 |
|-------|---------|---------|------------|
| employee_id | VARCHAR(20) | 中 | 部分遮盖 |
| name | VARCHAR(50) | 中 | 部分遮盖 |
| department | VARCHAR(50) | 低 | 不需脱敏 |
| position | VARCHAR(50) | 低 | 不需脱敏 |
| salary | DECIMAL(12, 2) | 高 | 数值范围化或随机化 |
| bonus | DECIMAL(12, 2) | 高 | 数值范围化或随机化 |
| bank_account | VARCHAR(20) | 高 | 部分遮盖 |
| social_security | VARCHAR(20) | 高 | 部分遮盖 |
| performance_rating | DECIMAL(3, 1) | 中 | 范围化或随机化 |

### 5. 在线交易表 (online_transactions)

包含电子商务和支付信息，适用于测试交易数据脱敏规则。

| 字段名 | 数据类型 | 敏感程度 | 适用脱敏方式 |
|-------|---------|---------|------------|
| order_id | VARCHAR(20) | 低 | 不需脱敏 |
| product_name | VARCHAR(100) | 低 | 不需脱敏 |
| quantity | INT | 低 | 不需脱敏 |
| price | DECIMAL(10, 2) | 中 | 可能需要范围化 |
| payment_method | VARCHAR(20) | 中 | 可能需要脱敏 |
| card_last_four | VARCHAR(4) | 中 | 可能需要完全遮盖 |
| shipping_address | VARCHAR(200) | 高 | 部分遮盖或地址泛化 |
| ip_address | VARCHAR(15) | 高 | 部分遮盖或随机化 |

## 脱敏规则应用场景

### 1. 静态数据脱敏场景

- **数据导出**：导出数据到测试环境或第三方系统时
- **数据分析**：提供给数据分析团队进行统计分析时
- **数据备份**：创建数据备份时

### 2. 动态数据脱敏场景

- **查询结果过滤**：根据用户权限动态脱敏查询结果
- **API响应处理**：对外部API调用返回的数据进行脱敏
- **日志记录**：记录系统日志时对敏感信息进行脱敏

### 3. 常用脱敏方法

1. **遮盖（Masking）**：用特定字符（如*）替换部分或全部敏感数据
2. **截断（Truncation）**：只显示数据的一部分
3. **替换（Substitution）**：用假数据替换真实数据
4. **随机化（Randomization）**：用随机值替换敏感数据
5. **范围化（Generalization）**：将精确值替换为范围值
6. **置换（Shuffling）**：在同一列内打乱数据
7. **加密（Encryption）**：使用加密算法处理数据
8. **令牌化（Tokenization）**：用无意义的令牌替换敏感数据

## 数据初始化

系统在启动时会自动执行`src/main/resources/db/init-test-data.sql`脚本，创建测试表并插入测试数据。

如需手动初始化数据，可以执行以下步骤：

1. 确保MySQL数据库已启动
2. 确保application.properties中的数据库连接配置正确
3. 启动应用程序，数据初始化器将自动执行SQL脚本 