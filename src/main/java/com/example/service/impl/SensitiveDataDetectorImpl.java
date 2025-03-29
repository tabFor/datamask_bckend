package com.example.service.impl;

import com.example.model.SensitiveColumn;
import com.example.service.SensitiveDataDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SensitiveDataDetectorImpl implements SensitiveDataDetector {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 敏感数据模式定义
    private static final Map<String, Pattern> SENSITIVE_PATTERNS = Map.of(
        "身份证号", Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"),
        "手机号", Pattern.compile("^1[3-9]\\d{9}$"),
        "邮箱", Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"),
        "银行卡号", Pattern.compile("^[1-9]\\d{9,29}$")
    );

    // 敏感列名关键词
    private static final List<String> SENSITIVE_KEYWORDS = List.of(
        "idcard", "身份证", "身份证号",
        "phone", "mobile", "手机", "手机号",
        "email", "mail", "邮箱",
        "bank", "card", "银行卡", "账号",
        "password", "密码",
        "address", "地址",
        "name", "姓名"
    );

    @Override
    public List<SensitiveColumn> detectSensitiveColumns(String tableName) {
        List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
        
        // 获取表结构信息
        String sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, tableName);
        
        for (Map<String, Object> column : columns) {
            String columnName = (String) column.get("column_name");
            String dataType = (String) column.get("data_type");
            
            SensitiveColumn sensitiveColumn = detectSensitiveColumn(columnName, dataType);
            if (sensitiveColumn != null) {
                sensitiveColumn.setTableName(tableName);
                sensitiveColumns.add(sensitiveColumn);
            }
        }
        
        return sensitiveColumns;
    }

    @Override
    public List<SensitiveColumn> detectAllSensitiveColumns() {
        List<SensitiveColumn> allSensitiveColumns = new ArrayList<>();
        
        // 获取所有表名
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";
        List<String> tables = jdbcTemplate.queryForList(sql, String.class);
        
        for (String table : tables) {
            allSensitiveColumns.addAll(detectSensitiveColumns(table));
        }
        
        return allSensitiveColumns;
    }

    @Override
    public SensitiveColumn detectSensitiveColumn(String columnName, String dataType) {
        // 检查列名是否包含敏感关键词
        String lowerColumnName = columnName.toLowerCase();
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lowerColumnName.contains(keyword.toLowerCase())) {
                SensitiveColumn sensitiveColumn = new SensitiveColumn();
                sensitiveColumn.setColumnName(columnName);
                sensitiveColumn.setDataType(dataType);
                sensitiveColumn.setSensitiveType(determineSensitiveType(columnName, dataType));
                sensitiveColumn.setMaskingRule(determineMaskingRule(sensitiveColumn.getSensitiveType()));
                sensitiveColumn.setDescription("通过列名关键词匹配发现");
                sensitiveColumn.setEnabled(true);
                return sensitiveColumn;
            }
        }
        
        // 如果列名不包含敏感关键词，检查数据类型是否匹配敏感数据模式
        if ("varchar".equalsIgnoreCase(dataType) || "char".equalsIgnoreCase(dataType) || 
            "text".equalsIgnoreCase(dataType) || "longtext".equalsIgnoreCase(dataType)) {
            
            // 获取列的数据样本
            String sampleSql = "SELECT " + columnName + " FROM " + columnName + " LIMIT 1";
            try {
                String sample = jdbcTemplate.queryForObject(sampleSql, String.class);
                if (sample != null) {
                    for (Map.Entry<String, Pattern> entry : SENSITIVE_PATTERNS.entrySet()) {
                        if (entry.getValue().matcher(sample).matches()) {
                            SensitiveColumn sensitiveColumn = new SensitiveColumn();
                            sensitiveColumn.setColumnName(columnName);
                            sensitiveColumn.setDataType(dataType);
                            sensitiveColumn.setSensitiveType(entry.getKey());
                            sensitiveColumn.setMaskingRule(determineMaskingRule(entry.getKey()));
                            sensitiveColumn.setDescription("通过数据模式匹配发现");
                            sensitiveColumn.setEnabled(true);
                            return sensitiveColumn;
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略查询错误，继续检查其他列
            }
        }
        
        return null;
    }

    private String determineSensitiveType(String columnName, String dataType) {
        String lowerColumnName = columnName.toLowerCase();
        
        if (lowerColumnName.contains("idcard") || lowerColumnName.contains("身份证")) {
            return "身份证号";
        } else if (lowerColumnName.contains("phone") || lowerColumnName.contains("mobile") || 
                   lowerColumnName.contains("手机")) {
            return "手机号";
        } else if (lowerColumnName.contains("email") || lowerColumnName.contains("mail") || 
                   lowerColumnName.contains("邮箱")) {
            return "邮箱";
        } else if (lowerColumnName.contains("bank") || lowerColumnName.contains("card") || 
                   lowerColumnName.contains("银行卡")) {
            return "银行卡号";
        } else if (lowerColumnName.contains("password") || lowerColumnName.contains("密码")) {
            return "密码";
        } else if (lowerColumnName.contains("address") || lowerColumnName.contains("地址")) {
            return "地址";
        } else if (lowerColumnName.contains("name") || lowerColumnName.contains("姓名")) {
            return "姓名";
        }
        
        return "其他敏感信息";
    }

    private String determineMaskingRule(String sensitiveType) {
        switch (sensitiveType) {
            case "身份证号":
                return "保留前6位和后4位，中间用*代替";
            case "手机号":
                return "保留前3位和后4位，中间用*代替";
            case "邮箱":
                return "保留@前2位和@后完整域名，中间用*代替";
            case "银行卡号":
                return "保留前6位和后4位，中间用*代替";
            case "密码":
                return "全部替换为*";
            case "地址":
                return "保留省市区，详细地址用*代替";
            case "姓名":
                return "保留姓氏，名字用*代替";
            default:
                return "默认脱敏规则";
        }
    }
} 