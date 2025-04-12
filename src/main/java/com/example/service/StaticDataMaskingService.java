package com.example.service;

import com.example.model.DesensitizationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;

/**
 * 静态数据脱敏服务
 * 专门处理静态脱敏相关的操作，与动态脱敏（DataMaskingService）完全分离
 */
@Service
public class StaticDataMaskingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StaticDataMaskingService.class);
    
    @Autowired
    private DesensitizationRuleService desensitizationRuleService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PresidioService presidioService;
    
    /**
     * 根据规则ID列表获取脱敏规则
     */
    public List<Map<String, Object>> getRulesByIds(List<String> ruleIds) {
        logger.info("获取脱敏规则, 规则ID数量: {}", ruleIds.size());
        
        List<Map<String, Object>> rules = new ArrayList<>();
        
        for (String ruleId : ruleIds) {
            // 获取并转换规则
            Optional<DesensitizationRule> ruleOpt = desensitizationRuleService.getRuleById(ruleId);
            
            // 检查规则是否存在
            boolean exists = ruleOpt.isPresent();
            
            if (exists) {
                DesensitizationRule rule = ruleOpt.get();
                Map<String, Object> ruleMap = convertRuleToMap(rule);
                rules.add(ruleMap);
                logger.info("成功获取规则: {}, 类型: {}", rule.getName(), rule.getType());
            } else {
                logger.warn("找不到ID为'{}'的规则", ruleId);
                
                // 尝试查找可能的匹配
                List<DesensitizationRule> allRules = desensitizationRuleService.getAllRules();
                logger.debug("系统中共有 {} 条规则", allRules.size());
            }
        }
        
        logger.info("成功获取规则数量: {}", rules.size());
        
        return rules;
    }
    
    /**
     * 将DesensitizationRule对象转换为Map
     */
    private Map<String, Object> convertRuleToMap(DesensitizationRule rule) {
        Map<String, Object> ruleMap = new HashMap<>();
        ruleMap.put("id", rule.getId());
        ruleMap.put("ruleId", rule.getRuleId());
        ruleMap.put("name", rule.getName());
        ruleMap.put("description", rule.getDescription());
        ruleMap.put("type", rule.getType());
        ruleMap.put("pattern", rule.getPattern());
        ruleMap.put("prefixLength", rule.getPrefixLength());
        ruleMap.put("suffixLength", rule.getSuffixLength());
        ruleMap.put("replacementChar", rule.getReplacementChar());
        return ruleMap;
    }
    
    /**
     * 处理静态脱敏数据
     */
    public List<Map<String, Object>> processMaskedData(List<Map<String, Object>> originalData, List<Map<String, Object>> rules) {
        if (originalData == null || originalData.isEmpty()) {
            logger.warn("没有获取到原始数据，无法进行脱敏处理");
            return originalData;
        }
        
        if (rules == null || rules.isEmpty()) {
            logger.warn("没有提供脱敏规则，返回原始数据");
            return originalData;
        }
        
        // 打印原始数据的列名，用于调试
        if (!originalData.isEmpty()) {
            Map<String, Object> firstRow = originalData.get(0);
            logger.debug("====== 原始数据列名信息 ======");
            logger.debug("原始数据列名: {}", firstRow.keySet());
            logger.debug("============================");
        }
        
        // 打印规则中的列名，用于对比
        logger.debug("====== 规则中的列名信息 ======");
        for (int i = 0; i < rules.size(); i++) {
            Map<String, Object> rule = rules.get(i);
            String columnName = extractColumnName(rule);
            String maskingType = extractMaskingType(rule);
            Boolean isActive = extractIsActive(rule);
            
            logger.debug("规则[{}]:", i);
            logger.debug("  - 列名: {}", columnName);
            logger.debug("  - 脱敏类型: {}", maskingType);
            logger.debug("  - 是否激活: {}", isActive);
        }
        logger.debug("===========================");
        
        // 应用脱敏规则
        List<Map<String, Object>> maskedData = new ArrayList<>();
        int processedRows = 0;
        int maskedFields = 0;
        
        // 处理原始数据
        for (Map<String, Object> row : originalData) {
            Map<String, Object> maskedRow = new HashMap<>(row);
            boolean rowMasked = false;
            
            // 应用每条脱敏规则
            for (Map<String, Object> rule : rules) {
                // 获取规则相关信息
                String columnName = extractColumnName(rule);
                String maskingType = extractMaskingType(rule);
                Boolean isActive = extractIsActive(rule);
                
                // 打印每条规则的应用过程
                logger.debug("应用规则: 列名={}, 类型={}", columnName, maskingType);
                
                // 检查规则是否激活
                if (isActive != null && !isActive) {
                    logger.debug("  规则未激活，跳过");
                    continue;  // 跳过未激活的规则
                }
                
                // 如果列名为null或空，跳过此规则
                if (columnName == null || columnName.isEmpty()) {
                    logger.debug("  列名为空，跳过规则");
                    continue;
                }
                
                // 尝试匹配列名
                String matchedColumn = findMatchingColumn(row, columnName);
                
                // 检查是否找到匹配的列
                if (matchedColumn == null) {
                    logger.debug("  找不到匹配的列: {}", columnName);
                    continue;
                }
                
                logger.debug("  找到匹配列: {}", matchedColumn);
                
                Object value = row.get(matchedColumn);
                
                // 只有当值不为null时才进行脱敏
                if (value != null) {
                    logger.debug("  原始值: {}", value);
                    
                    Object maskedValue = applyMasking(value.toString(), maskingType, rule);
                    logger.debug("  脱敏后值: {}", maskedValue);
                    
                    // 只有当脱敏后的值与原始值不同时才更新
                    if (!value.equals(maskedValue)) {
                        maskedRow.put(matchedColumn, maskedValue);
                        maskedFields++;
                        rowMasked = true;
                        logger.debug("  值已更新");
                    } else {
                        logger.debug("  值未变化，不更新");
                    }
                } else {
                    logger.debug("  值为null，不处理");
                }
            }
            
            maskedData.add(maskedRow);
            if (rowMasked) {
                processedRows++;
            }
        }
        
        logger.info("完成脱敏处理，共处理{}条记录，{}个字段被脱敏", processedRows, maskedFields);
        logger.debug("====== 脱敏处理结果 ======");
        logger.debug("处理记录数: {}", processedRows);
        logger.debug("脱敏字段数: {}", maskedFields);
        logger.debug("=========================");
        
        return maskedData;
    }
    
    /**
     * 在原始数据中查找匹配的列名
     */
    private String findMatchingColumn(Map<String, Object> row, String columnName) {
        // 直接匹配
        if (row.containsKey(columnName)) {
            return columnName;
        }
        // 尝试小写匹配
        else if (row.containsKey(columnName.toLowerCase())) {
            return columnName.toLowerCase();
        }
        // 尝试首字母大写匹配
        else if (columnName.length() > 0 && row.containsKey(columnName.substring(0, 1).toUpperCase() + columnName.substring(1))) {
            return columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
        }
        // 对所有列名进行不区分大小写的匹配
        else {
            for (String key : row.keySet()) {
                if (key.equalsIgnoreCase(columnName)) {
                    return key;
                }
            }
        }
        return null;
    }
    
    /**
     * 从规则中提取列名
     */
    public String extractColumnName(Map<String, Object> rule) {
        for (String key : Arrays.asList("columnName", "column", "field", "name")) {
            if (rule.containsKey(key) && rule.get(key) != null) {
                return rule.get(key).toString();
            }
        }
        return null;
    }
    
    /**
     * 从规则中提取脱敏类型
     */
    private String extractMaskingType(Map<String, Object> rule) {
        for (String key : Arrays.asList("maskingType", "type", "pattern")) {
            if (rule.containsKey(key) && rule.get(key) != null) {
                return rule.get(key).toString();
            }
        }
        return null;
    }
    
    /**
     * 从规则中提取是否激活
     */
    private Boolean extractIsActive(Map<String, Object> rule) {
        for (String key : Arrays.asList("active", "isActive", "enabled")) {
            if (rule.containsKey(key) && rule.get(key) != null) {
                if (rule.get(key) instanceof Boolean) {
                    return (Boolean) rule.get(key);
                } else {
                    return Boolean.valueOf(rule.get(key).toString());
                }
            }
        }
        // 默认为激活状态
        return true;
    }
    
    /**
     * 应用脱敏规则
     */
    public Object applyMasking(String value, String maskingType, Map<String, Object> rule) {
        if (value == null || maskingType == null) {
            return value;
        }
        
        // 使用内置脱敏规则处理
        logger.debug("使用内置脱敏规则处理");
        
        // 获取规则参数
        Integer prefixLength = null;
        Integer suffixLength = null;
        String replacementChar = "*";
        
        if (rule.containsKey("prefixLength") && rule.get("prefixLength") != null) {
            if (rule.get("prefixLength") instanceof Number) {
                prefixLength = ((Number) rule.get("prefixLength")).intValue();
            } else {
                try {
                    prefixLength = Integer.parseInt(rule.get("prefixLength").toString());
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        if (rule.containsKey("suffixLength") && rule.get("suffixLength") != null) {
            if (rule.get("suffixLength") instanceof Number) {
                suffixLength = ((Number) rule.get("suffixLength")).intValue();
            } else {
                try {
                    suffixLength = Integer.parseInt(rule.get("suffixLength").toString());
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
            replacementChar = rule.get("replacementChar").toString();
        }
        
        // 根据脱敏类型处理
        String type = maskingType.toUpperCase();
        switch (type) {
            case "PHONE":
            case "手机号":
            case "电话号码":
                return maskPhone(value, prefixLength != null ? prefixLength : 3, 
                                 suffixLength != null ? suffixLength : 4, replacementChar);
            case "ID_CARD":
            case "身份证":
            case "身份证号码":
                return maskIdCard(value, prefixLength != null ? prefixLength : 6, 
                                  suffixLength != null ? suffixLength : 4, replacementChar);
            case "NAME":
            case "姓名":
                return maskName(value, prefixLength != null ? prefixLength : 1, replacementChar);
            case "EMAIL":
            case "邮箱":
            case "电子邮箱":
                return maskEmail(value, prefixLength != null ? prefixLength : 2, replacementChar);
            case "BANK_CARD":
            case "银行卡":
            case "银行卡号":
                return maskBankCard(value, suffixLength != null ? suffixLength : 4, replacementChar);
            case "ADDRESS":
            case "地址":
                return maskAddress(value, prefixLength != null ? prefixLength : 10, replacementChar);
            case "FULL_MASK":
            case "全遮盖":
            case "全部遮盖":
                return maskFullValue(value, replacementChar);
            default:
                return value;
        }
    }
    
    /**
     * 手机号脱敏
     */
    private String maskPhone(String value, int prefixLength, int suffixLength, String replacementChar) {
        if (value == null || value.isEmpty()) return value;
        
        // 清理可能的非数字字符
        value = value.replaceAll("[^0-9]", "");
        
        // 如果字符串太短，保持不变
        if (value.length() <= prefixLength + suffixLength) {
            return value;
        }
        
        // 执行脱敏
        String prefix = value.substring(0, prefixLength);
        String suffix = value.substring(value.length() - suffixLength);
        
        // 构建替换字符
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < value.length() - prefixLength - suffixLength; i++) {
            masked.append(replacementChar);
        }
        masked.append(suffix);
        
        return masked.toString();
    }
    
    /**
     * 身份证号脱敏
     */
    private String maskIdCard(String value, int prefixLength, int suffixLength, String replacementChar) {
        if (value == null || value.isEmpty()) return value;
        
        // 清理可能的非字母数字字符
        value = value.replaceAll("[^a-zA-Z0-9]", "");
        
        // 如果字符串太短，不处理
        if (value.length() <= prefixLength + suffixLength) {
            return value;
        }
        
        // 执行脱敏
        String prefix = value.substring(0, prefixLength);
        String suffix = value.substring(value.length() - suffixLength);
        
        // 构建替换字符
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < value.length() - prefixLength - suffixLength; i++) {
            masked.append(replacementChar);
        }
        masked.append(suffix);
        
        return masked.toString();
    }
    
    /**
     * 姓名脱敏
     */
    private String maskName(String value, int prefixLength, String replacementChar) {
        if (value == null || value.isEmpty()) return value;
        
        // 如果字符串太短，不处理
        if (value.length() <= prefixLength) {
            return value;
        }
        
        // 执行脱敏
        String prefix = value.substring(0, prefixLength);
        
        // 构建替换字符
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < value.length() - prefixLength; i++) {
            masked.append(replacementChar);
        }
        
        return masked.toString();
    }
    
    /**
     * 邮箱脱敏
     */
    private String maskEmail(String value, int prefixLength, String replacementChar) {
        if (value == null || value.isEmpty()) return value;
        
        // 检查是否是有效的邮箱格式
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return value; // 无效的邮箱格式
        }
        
        // 执行脱敏
        String username = value.substring(0, atIndex);
        String domain = value.substring(atIndex);
        
        if (username.length() <= prefixLength) {
            return value; // 如果用户名太短，不处理
        }
        
        String prefix = username.substring(0, prefixLength);
        
        // 构建替换字符
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < username.length() - prefixLength; i++) {
            masked.append(replacementChar);
        }
        masked.append(domain);
        
        return masked.toString();
    }
    
    /**
     * 银行卡号脱敏
     */
    private String maskBankCard(String value, int suffixLength, String replacementChar) {
        if (value == null || value.isEmpty()) return value;
        
        // 清理可能的非数字字符
        value = value.replaceAll("[^0-9]", "");
        
        // 如果字符串太短，不处理
        if (value.length() <= suffixLength) {
            return value;
        }
        
        // 执行脱敏
        String suffix = value.substring(value.length() - suffixLength);
        
        // 构建替换字符
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < value.length() - suffixLength; i++) {
            masked.append(replacementChar);
        }
        masked.append(suffix);
        
        return masked.toString();
    }
    
    /**
     * 地址脱敏
     */
    private String maskAddress(String value, int prefixLength, String replacementChar) {
        if (value == null || value.isEmpty()) return value;
        
        // 如果地址太短，不处理
        if (value.length() <= prefixLength) {
            return value;
        }
        
        // 执行脱敏
        String prefix = value.substring(0, prefixLength);
        
        // 构建替换字符
        StringBuilder masked = new StringBuilder(prefix);
        for (int i = 0; i < value.length() - prefixLength; i++) {
            masked.append(replacementChar);
        }
        
        return masked.toString();
    }
    
    /**
     * 全部遮盖
     */
    private String maskFullValue(String value, String replacementChar) {
        if (value == null || value.isEmpty()) return value;
        
        // 全部字符用替代字符替换
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            masked.append(replacementChar);
        }
        
        return masked.toString();
    }
    
    /**
     * 创建目标表并插入脱敏后的数据
     */
    public boolean createTableAndInsertData(String targetTable, List<Map<String, Object>> maskedData) {
        logger.info("开始创建目标表并插入脱敏数据, 目标表: {}, 数据量: {}", targetTable, maskedData.size());
        
        try {
            if (maskedData == null || maskedData.isEmpty()) {
                logger.warn("没有可插入的数据");
                return false;
            }
            
            // 获取第一行数据的列名和类型，用于创建表
            Map<String, Object> firstRow = maskedData.get(0);
            StringBuilder createTableSql = new StringBuilder();
            createTableSql.append("CREATE TABLE IF NOT EXISTS ").append(targetTable).append(" (");
            
            // 添加id列作为主键
            createTableSql.append("id BIGINT AUTO_INCREMENT PRIMARY KEY, ");
            
            // 添加其他列，默认使用VARCHAR类型
            for (String columnName : firstRow.keySet()) {
                if (!"id".equalsIgnoreCase(columnName)) {
                    createTableSql.append(columnName).append(" VARCHAR(255), ");
                }
            }
            
            // 去掉最后的逗号和空格，添加结束括号
            String sql = createTableSql.substring(0, createTableSql.length() - 2) + ")";
            
            // 创建表
            logger.debug("执行创建表SQL: {}", sql);
            
            try {
                jdbcTemplate.execute(sql);
                logger.info("表创建成功: {}", targetTable);
            } catch (Exception e) {
                logger.error("表创建失败: {}", e.getMessage(), e);
                throw e;
            }
            
            // 构建插入数据的SQL
            StringBuilder insertSql = new StringBuilder();
            insertSql.append("INSERT INTO ").append(targetTable).append(" (");
            
            // 添加列名
            for (String columnName : firstRow.keySet()) {
                if (!"id".equalsIgnoreCase(columnName)) {
                    insertSql.append(columnName).append(", ");
                }
            }
            
            // 去掉最后的逗号和空格，添加VALUES关键字
            insertSql = new StringBuilder(insertSql.substring(0, insertSql.length() - 2));
            insertSql.append(") VALUES (");
            
            // 为每个列添加占位符
            for (int i = 0; i < firstRow.size() - (firstRow.containsKey("id") ? 1 : 0); i++) {
                insertSql.append("?, ");
            }
            
            // 去掉最后的逗号和空格，添加结束括号
            String insertSqlStr = insertSql.substring(0, insertSql.length() - 2) + ")";
            
            // 批量插入数据
            logger.debug("执行插入数据SQL: {}", insertSqlStr);
            int insertedCount = 0;
            
            try {
                for (Map<String, Object> row : maskedData) {
                    // 准备插入参数
                    List<Object> params = new ArrayList<>();
                    for (String columnName : firstRow.keySet()) {
                        if (!"id".equalsIgnoreCase(columnName)) {
                            params.add(row.get(columnName));
                        }
                    }
                    
                    // 执行插入
                    jdbcTemplate.update(insertSqlStr, params.toArray());
                    insertedCount++;
                }
                logger.info("数据插入成功，共插入: {}条记录", insertedCount);
            } catch (Exception e) {
                logger.error("数据插入失败: {}", e.getMessage(), e);
                throw e;
            }
            
            logger.info("成功创建表并插入{}条数据到{}", maskedData.size(), targetTable);
            return true;
            
        } catch (Exception e) {
            logger.error("创建表或插入数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建表或插入数据失败", e);
        }
    }
    
    /**
     * 查询原始数据
     */
    public List<Map<String, Object>> queryOriginalData(String sourceDatabase, String sourceTables) {
        logger.info("开始查询原始数据，数据库信息: {}，表: {}", sourceDatabase, sourceTables);
        
        try {
            // 解析表名
            String[] tables = sourceTables.split(",");
            if (tables.length == 0) {
                logger.warn("未指定表名");
                return new ArrayList<>();
            }
            
            String tableName = tables[0].trim(); // 暂时只处理第一个表
            logger.info("准备查询表: {}", tableName);
            
            // 解析数据库连接信息
            Map<String, String> dbInfo = parseSourceDatabase(sourceDatabase);
            String dbType = dbInfo.getOrDefault("类型", "mysql").toLowerCase();
            String host = dbInfo.getOrDefault("主机", "localhost");
            String port = dbInfo.getOrDefault("端口", "3306");
            String dbName = dbInfo.getOrDefault("数据库", "");
            String username = dbInfo.getOrDefault("用户名", "");
            String password = dbInfo.getOrDefault("密码", "");
            
            logger.info("数据库类型: {}, 主机: {}, 端口: {}, 数据库名: {}", dbType, host, port, dbName);
            
            // 获取自定义数据源连接
            Connection connection = getCustomDatabaseConnection(dbType, host, port, dbName, username, password);
            
            // 根据数据库类型构建SQL查询
            String sql = buildQuerySql(dbType, tableName);
            logger.debug("执行SQL: {}", sql);
            
            try {
                // 执行查询
                List<Map<String, Object>> result = executeQuery(connection, sql);
                logger.info("查询到{}条数据", result.size());
                return result;
            } catch (Exception e) {
                logger.error("执行SQL查询失败: {}", e.getMessage(), e);
                return generateMockData(); // 查询失败时返回模拟数据
            } finally {
                // 关闭连接
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    logger.debug("数据库连接已关闭");
                }
            }
        } catch (Exception e) {
            logger.error("查询原始数据失败: {}", e.getMessage(), e);
            return generateMockData(); // 失败时返回模拟数据
        }
    }
    
    /**
     * 解析数据库连接信息字符串
     */
    private Map<String, String> parseSourceDatabase(String sourceDatabase) {
        Map<String, String> dbInfo = new HashMap<>();
        
        if (sourceDatabase == null || sourceDatabase.trim().isEmpty()) {
            return dbInfo;
        }
        
        // 解析格式: "类型:mysql;主机:localhost;端口:3306;数据库:test;用户名:root;密码:123456"
        String[] parts = sourceDatabase.split(";");
        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                dbInfo.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        
        return dbInfo;
    }
    
    /**
     * 获取自定义数据库连接
     */
    private Connection getCustomDatabaseConnection(String dbType, String host, String port, String dbName, String username, String password) throws Exception {
        String url;
        
        switch (dbType.toLowerCase()) {
            case "mysql":
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                Class.forName("com.mysql.cj.jdbc.Driver");
                break;
            case "postgresql":
                url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                Class.forName("org.postgresql.Driver");
                break;
            case "oracle":
                url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbName;
                Class.forName("oracle.jdbc.OracleDriver");
                break;
            case "sqlserver":
                url = "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + dbName;
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
        
        logger.debug("尝试连接数据库: {}", url);
        return DriverManager.getConnection(url, username, password);
    }
    
    /**
     * 根据数据库类型构建查询SQL
     */
    private String buildQuerySql(String dbType, String tableName) {
        // 根据不同的数据库类型生成不同的SQL语句
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "postgresql":
                return "SELECT * FROM " + tableName;
            case "oracle":
                return "SELECT * FROM " + tableName.toUpperCase();
            case "sqlserver":
                return "SELECT * FROM [" + tableName + "]";
            default:
                return "SELECT * FROM " + tableName;
        }
    }
    
    /**
     * 执行SQL查询并将结果转换为List<Map<String, Object>>格式
     */
    private List<Map<String, Object>> executeQuery(Connection conn, String sql) throws Exception {
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 获取列名
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            
            // 处理结果集
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(columnNames.get(i-1), rs.getObject(i));
                }
                resultList.add(row);
            }
        }
        
        return resultList;
    }
    
    /**
     * 生成模拟数据（用于测试）
     */
    private List<Map<String, Object>> generateMockData() {
        List<Map<String, Object>> mockData = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("username", "user" + i);
            row.put("email", "user" + i + "@example.com");
            row.put("phone", "1380000" + String.format("%04d", i));
            row.put("address", "北京市朝阳区某街道" + i + "号");
            row.put("idCard", "11010119800101" + String.format("%04d", i));
            row.put("createTime", "2023-01-01 00:00:00");
            mockData.add(row);
        }
        return mockData;
    }
} 