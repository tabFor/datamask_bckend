-- 在masking_rules表中添加masking_mode字段
ALTER TABLE masking_rules ADD COLUMN masking_mode VARCHAR(50) NOT NULL DEFAULT 'BUILT_IN'; 