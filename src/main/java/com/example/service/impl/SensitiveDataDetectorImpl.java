package com.example.service.impl;

import com.example.model.SensitiveColumn;
import com.example.service.SensitiveDataDetector;
import com.example.service.PresidioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.HashMap;

@Service
public class SensitiveDataDetectorImpl implements SensitiveDataDetector {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveDataDetectorImpl.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PresidioService presidioService;
    
    @Autowired
    private ObjectMapper objectMapper;

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

    @Override
    public List<SensitiveColumn> detectSensitiveColumnsWithPresidio(String tableName) {
        List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
        
        try {
            // 查询表结构
            String sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, tableName);
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("column_name");
                String dataType = (String) column.get("data_type");
                
                // 只处理文本类型的列
                if (isTextType(dataType)) {
                    // 获取列数据样本
                    List<String> samples = getSampleData(tableName, columnName, 10);
                    
                    // 使用Presidio分析样本数据
                    Map<String, Integer> entityTypes = analyzeColumnSamples(samples);
                    
                    // 如果检测到敏感实体，创建敏感列
                    if (!entityTypes.isEmpty()) {
                        String sensitiveType = getMostFrequentEntityType(entityTypes);
                        String maskingRule = determineMaskingRule(sensitiveType);
                        
                        SensitiveColumn sensitiveColumn = new SensitiveColumn();
                        sensitiveColumn.setTableName(tableName);
                        sensitiveColumn.setColumnName(columnName);
                        sensitiveColumn.setDataType(dataType);
                        sensitiveColumn.setSensitiveType(sensitiveType);
                        sensitiveColumn.setMaskingRule(maskingRule);
                        sensitiveColumn.setDescription("Presidio自动识别: " + sensitiveType);
                        sensitiveColumn.setEnabled(true);
                        
                        sensitiveColumns.add(sensitiveColumn);
                        
                        logger.info("Presidio识别出敏感列: {}.{} - 类型: {}", tableName, columnName, sensitiveType);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Presidio自动识别敏感列失败", e);
        }
        
        return sensitiveColumns;
    }
    
    /**
     * 判断数据类型是否为文本类型
     */
    private boolean isTextType(String dataType) {
        if (dataType == null) return false;
        
        dataType = dataType.toLowerCase();
        return dataType.contains("char") || 
               dataType.contains("text") || 
               dataType.contains("varchar") || 
               dataType.contains("string");
    }
    
    /**
     * 获取列的样本数据
     */
    private List<String> getSampleData(String tableName, String columnName, int sampleSize) {
        try {
            String sql = String.format("SELECT DISTINCT %s FROM %s WHERE %s IS NOT NULL LIMIT ?", 
                                      columnName, tableName, columnName);
            
            return jdbcTemplate.queryForList(sql, String.class, sampleSize);
        } catch (Exception e) {
            logger.warn("获取列样本数据失败: {}.{}", tableName, columnName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 分析列样本数据，返回检测到的实体类型及出现次数
     */
    private Map<String, Integer> analyzeColumnSamples(List<String> samples) {
        Map<String, Integer> entityCounts = new HashMap<>();
        
        for (String sample : samples) {
            if (sample != null && !sample.isEmpty()) {
                try {
                    // 使用Presidio分析
                    List<Map<String, Object>> results = presidioService.analyzeText(sample);
                    
                    for (Map<String, Object> entity : results) {
                        String type = (String) entity.get("entity_type");
                        
                        // 更新实体类型计数
                        entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
                    }
                } catch (Exception e) {
                    logger.warn("分析样本失败: {}", sample, e);
                }
            }
        }
        
        return entityCounts;
    }
    
    /**
     * 获取出现次数最多的实体类型
     */
    private String getMostFrequentEntityType(Map<String, Integer> entityCounts) {
        return entityCounts.entrySet().stream()
                          .max(Map.Entry.comparingByValue())
                          .map(Map.Entry::getKey)
                          .orElse("UNKNOWN");
    }
    
    /**
     * 将Presidio实体类型映射到系统中的敏感类型
     */
    private String mapEntityTypeToSensitiveType(String entityType) {
        switch (entityType) {
            case "PERSON":
                return "姓名";
            case "EMAIL_ADDRESS":
                return "邮箱";
            case "PHONE_NUMBER":
                return "手机号";
            case "CREDIT_CARD":
            case "BANK_CARD":
                return "银行卡号";
            case "CHINA_ID":
            case "US_SSN":
                return "身份证号";
            case "PASSWORD":
                return "密码";
            case "LOCATION":
            case "ADDRESS":
                return "地址";
            default:
                return entityType;
        }
    }

    /**
     * 使用自定义数据库连接检测敏感列
     */
    @Override
    public List<SensitiveColumn> detectSensitiveColumnsWithConnection(Connection connection, String tableName) {
        List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
        
        try {
            // 判断数据库类型
            String dbType = connection.getMetaData().getDatabaseProductName().toLowerCase();
            logger.info("数据库类型: {}", dbType);
            
            // 根据数据库类型选择不同的查询
            String sql;
            if (dbType.contains("postgresql")) {
                sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ? AND table_schema = 'public'";
                logger.info("使用PostgreSQL查询: {}", sql);
            } else {
                sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
                logger.info("使用通用查询: {}", sql);
            }
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tableName);
                logger.info("执行查询表结构: {} 参数: {}", sql, tableName);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    int columnCount = 0;
                    while (rs.next()) {
                        columnCount++;
                        String columnName = rs.getString("column_name");
                        String dataType = rs.getString("data_type");
                        
                        logger.info("发现列: {}, 类型: {}", columnName, dataType);
                        
                        SensitiveColumn sensitiveColumn = detectSensitiveColumnWithConnection(connection, tableName, columnName, dataType);
                        if (sensitiveColumn != null) {
                            sensitiveColumn.setTableName(tableName);
                            sensitiveColumns.add(sensitiveColumn);
                            logger.info("检测到敏感列: {}.{} - 类型: {}", tableName, columnName, sensitiveColumn.getSensitiveType());
                        }
                    }
                    logger.info("表 {} 总共有 {} 个列", tableName, columnCount);
                }
            }
        } catch (Exception e) {
            logger.error("检测敏感列失败: " + e.getMessage(), e);
        }
        
        return sensitiveColumns;
    }
    
    /**
     * 基于列名和数据类型检测单个列是否敏感（使用自定义连接）
     */
    private SensitiveColumn detectSensitiveColumnWithConnection(Connection connection, String tableName, String columnName, String dataType) {
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
                logger.info("通过关键词匹配检测到敏感列: {}.{} - 类型: {}", tableName, columnName, sensitiveColumn.getSensitiveType());
                return sensitiveColumn;
            }
        }
        
        // 如果列名不包含敏感关键词，检查数据类型是否匹配敏感数据模式
        if (isTextType(dataType)) {
            // 获取列的数据样本
            try {
                // 获取数据库类型
                String dbType = connection.getMetaData().getDatabaseProductName().toLowerCase();
                
                // 构建查询语句，处理可能的引号问题
                String quotedTableName = tableName;
                String quotedColumnName = columnName;
                
                // PostgreSQL特殊处理
                if (dbType.contains("postgresql")) {
                    quotedTableName = "\"" + tableName + "\"";
                    quotedColumnName = "\"" + columnName + "\"";
                }
                
                String sampleSql = String.format("SELECT %s FROM %s WHERE %s IS NOT NULL LIMIT 1", 
                                   quotedColumnName, quotedTableName, quotedColumnName);
                
                logger.info("执行数据样本查询: {}", sampleSql);
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sampleSql)) {
                     
                    if (rs.next()) {
                        String sample = rs.getString(1);
                        logger.info("获取到列 {}.{} 的样本数据: {}", tableName, columnName, sample);
                        
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
                                    logger.info("通过模式匹配检测到敏感列: {}.{} - 类型: {}", tableName, columnName, entry.getKey());
                                    return sensitiveColumn;
                                }
                            }
                        }
                    } else {
                        logger.info("列 {}.{} 没有数据样本", tableName, columnName);
                    }
                }
            } catch (Exception e) {
                // 忽略查询错误，继续检查其他列
                logger.info("获取列数据样本失败: {}.{}: {}", tableName, columnName, e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 使用Presidio和自定义连接检测敏感列
     */
    @Override
    public List<SensitiveColumn> detectSensitiveColumnsWithPresidioAndConnection(Connection connection, String tableName) {
        List<SensitiveColumn> sensitiveColumns = new ArrayList<>();
        
        try {
            // 判断数据库类型
            String dbType = connection.getMetaData().getDatabaseProductName().toLowerCase();
            logger.info("Presidio检测 - 数据库类型: {}", dbType);
            
            // 根据数据库类型选择不同的查询
            String sql;
            if (dbType.contains("postgresql")) {
                sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ? AND table_schema = 'public'";
                logger.info("使用PostgreSQL查询: {}", sql);
            } else {
                sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
                logger.info("使用通用查询: {}", sql);
            }
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tableName);
                logger.info("执行查询表结构: {} 参数: {}", sql, tableName);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    int columnCount = 0;
                    while (rs.next()) {
                        columnCount++;
                        String columnName = rs.getString("column_name");
                        String dataType = rs.getString("data_type");
                        
                        logger.info("Presidio - 发现列: {}, 类型: {}", columnName, dataType);
                        
                        // 只处理文本类型的列
                        if (isTextType(dataType)) {
                            // 获取列数据样本
                            List<String> samples = getSampleDataWithConnection(connection, tableName, columnName, 10);
                            logger.info("Presidio - 获取到列 {}.{} 的 {} 个样本数据", tableName, columnName, samples.size());
                            
                            // 使用Presidio分析样本数据
                            Map<String, Integer> entityTypes = analyzeColumnSamples(samples);
                            
                            // 如果检测到敏感实体，创建敏感列
                            if (!entityTypes.isEmpty()) {
                                String sensitiveType = getMostFrequentEntityType(entityTypes);
                                String mappedType = mapEntityTypeToSensitiveType(sensitiveType);
                                String maskingRule = determineMaskingRule(mappedType);
                                
                                SensitiveColumn sensitiveColumn = new SensitiveColumn();
                                sensitiveColumn.setTableName(tableName);
                                sensitiveColumn.setColumnName(columnName);
                                sensitiveColumn.setDataType(dataType);
                                sensitiveColumn.setSensitiveType(mappedType);
                                sensitiveColumn.setMaskingRule(maskingRule);
                                sensitiveColumn.setDescription("Presidio自动识别: " + sensitiveType);
                                sensitiveColumn.setEnabled(true);
                                
                                sensitiveColumns.add(sensitiveColumn);
                                
                                logger.info("Presidio识别出敏感列: {}.{} - 类型: {}", tableName, columnName, mappedType);
                            } else {
                                logger.info("Presidio未在列 {}.{} 中识别出敏感数据", tableName, columnName);
                            }
                        }
                    }
                    logger.info("Presidio - 表 {} 总共有 {} 个列", tableName, columnCount);
                }
            }
        } catch (Exception e) {
            logger.error("Presidio自动识别敏感列失败", e);
        }
        
        return sensitiveColumns;
    }
    
    /**
     * 使用自定义连接获取列的样本数据
     */
    private List<String> getSampleDataWithConnection(Connection connection, String tableName, String columnName, int sampleSize) {
        List<String> samples = new ArrayList<>();
        
        try {
            // 获取数据库类型
            String dbType = connection.getMetaData().getDatabaseProductName().toLowerCase();
            
            // 构建查询语句，处理可能的引号问题
            String quotedTableName = tableName;
            String quotedColumnName = columnName;
            
            // PostgreSQL特殊处理
            if (dbType.contains("postgresql")) {
                quotedTableName = "\"" + tableName + "\"";
                quotedColumnName = "\"" + columnName + "\"";
            }
            
            String sql = String.format("SELECT DISTINCT %s FROM %s WHERE %s IS NOT NULL LIMIT ?", 
                                   quotedColumnName, quotedTableName, quotedColumnName);
            
            logger.info("执行样本数据查询: {}", sql);
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, sampleSize);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String value = rs.getString(1);
                        if (value != null && !value.isEmpty()) {
                            samples.add(value);
                            logger.debug("获取到样本数据: {}", value);
                        }
                    }
                }
            }
            
            logger.info("列 {}.{} 获取到 {} 个样本数据", tableName, columnName, samples.size());
        } catch (Exception e) {
            logger.warn("获取列样本数据失败: {}.{}: {}", tableName, columnName, e.getMessage(), e);
        }
        
        return samples;
    }
} 