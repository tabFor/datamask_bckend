package com.example.model;

import lombok.Data;

@Data
public class SensitiveColumn {
    private String tableName;
    private String columnName;
    private String dataType;
    private String sensitiveType; // 敏感类型：如身份证号、手机号、邮箱等
    private String maskingRule;   // 脱敏规则
    private String description;   // 描述
    private boolean isEnabled;    // 是否启用
} 