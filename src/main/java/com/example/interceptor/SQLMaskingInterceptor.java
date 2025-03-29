package com.example.interceptor;

import com.example.model.MaskingRuleEntity;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SQLMaskingInterceptor implements StatementInspector {
    
    private final Map<String, List<MaskingRuleEntity>> maskingRules = new ConcurrentHashMap<>();
    
    @Override
    public String inspect(String sql) {
        // 添加调试日志，查看是否拦截到了SQL
        System.out.println("SQLMaskingInterceptor: 拦截到SQL: " + sql);
        
        // 检查是否有脱敏规则
        System.out.println("SQLMaskingInterceptor: maskingRules.size() = " + maskingRules.size() + ", isEmpty = " + maskingRules.isEmpty());
        
        if (maskingRules.isEmpty()) {
            System.out.println("SQLMaskingInterceptor: 没有脱敏规则，原样返回SQL");
            return sql;
        }
        
        // 输出当前所有的脱敏规则
        printAllMaskingRules();
        
        // 解析SQL语句，识别表名和列名
        String tableName = extractTableName(sql);
        if (tableName == null) {
            System.out.println("SQLMaskingInterceptor: 无法提取表名，原样返回SQL");
            return sql;
        }
        
        // 获取该表的脱敏规则
        List<MaskingRuleEntity> rules = maskingRules.get(tableName);
        if (rules == null || rules.isEmpty()) {
            System.out.println("SQLMaskingInterceptor: 表 " + tableName + " 没有对应的脱敏规则，原样返回SQL");
            return sql;
        }
        
        // 根据规则修改SQL
        String modifiedSql = modifySQL(sql, rules);
        
        // 添加日志输出，方便调试
        System.out.println("Original SQL: " + sql);
        System.out.println("Modified SQL: " + modifiedSql);
        
        return modifiedSql;
    }
    
    private String extractTableName(String sql) {
        try {
            // 简单的SQL解析，提取表名
            System.out.println("开始从SQL中提取表名: " + sql);
            sql = sql.toLowerCase();
            
            // 处理SELECT语句
            if (sql.contains("from")) {
                int fromIndex = sql.indexOf("from ");
                if (fromIndex >= 0) {
                    String afterFrom = sql.substring(fromIndex + 5).trim();
                    // 提取表名 (处理可能的schema前缀和别名)
                    String[] parts = afterFrom.split("\\s+");
                    if (parts.length > 0) {
                        String tablePart = parts[0];
                        // 去除可能的schema前缀
                        if (tablePart.contains(".")) {
                            tablePart = tablePart.substring(tablePart.lastIndexOf(".") + 1);
                        }
                        System.out.println("从SQL中提取到表名: " + tablePart);
                        
                        // 不进行驼峰命名转换，直接返回原始表名
                        return tablePart;
                    }
                }
            }
            
            System.out.println("无法从SQL中提取表名");
            return null;
        } catch (Exception e) {
            System.err.println("提取表名时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String modifySQL(String sql, List<MaskingRuleEntity> rules) {
        try {
            // 根据脱敏规则修改SQL
            StringBuilder modifiedSql = new StringBuilder(sql);
            
            System.out.println("开始应用脱敏规则，规则数量: " + rules.size());
            for (MaskingRuleEntity rule : rules) {
                System.out.println("处理规则: " + rule.getTableName() + "." + rule.getColumnName() + ", 脱敏类型: " + rule.getMaskingType() + ", 活动状态: " + rule.isActive());
                
                if (!rule.isActive()) {
                    System.out.println("规则未激活，跳过");
                    continue;
                }
                
                String columnName = rule.getColumnName();
                String maskingType = rule.getMaskingType();
                
                // 识别SQL中可能的表别名
                String tableAlias = findTableAlias(sql, rule.getTableName());
                String qualifiedColumnName = columnName;
                if (tableAlias != null && !tableAlias.isEmpty()) {
                    qualifiedColumnName = tableAlias + "." + columnName;
                }
                
                // 根据不同的脱敏类型修改SQL
                try {
                    switch (maskingType) {
                        case "完全遮盖":
                            modifiedSql = replaceColumnWithMask(modifiedSql, qualifiedColumnName, "******");
                            break;
                        case "部分遮盖":
                            modifiedSql = replaceColumnWithPartialMask(modifiedSql, qualifiedColumnName);
                            break;
                        case "替换":
                            modifiedSql = replaceColumnWithValue(modifiedSql, qualifiedColumnName, "***");
                            break;
                        case "哈希":
                            modifiedSql = replaceColumnWithHash(modifiedSql, qualifiedColumnName);
                            break;
                        case "随机化":
                            modifiedSql = replaceColumnWithRandom(modifiedSql, qualifiedColumnName);
                            break;
                        default:
                            System.out.println("未知的脱敏类型: " + maskingType);
                            break;
                    }
                    System.out.println("应用脱敏规则后的SQL: " + modifiedSql);
                } catch (Exception e) {
                    System.err.println("应用脱敏规则时发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            return modifiedSql.toString();
        } catch (Exception e) {
            System.err.println("修改SQL出现一般错误: " + e.getMessage());
            e.printStackTrace();
            return sql; // 发生错误时返回原始SQL
        }
    }
    
    /**
     * 在SQL中查找表的别名
     * @param sql SQL语句
     * @param tableName 表名
     * @return 表别名
     */
    private String findTableAlias(String sql, String tableName) {
        String lowerSql = sql.toLowerCase();
        String lowerTableName = tableName.toLowerCase();
        
        // 查找形如 from tableName alias 或 join tableName alias 的模式
        String fromPattern = "from\\s+" + lowerTableName + "\\s+([a-z0-9_]+)";
        String joinPattern = "join\\s+" + lowerTableName + "\\s+([a-z0-9_]+)";
        
        // 首先尝试正则表达式查找
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(fromPattern);
        java.util.regex.Matcher matcher = pattern.matcher(lowerSql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        pattern = java.util.regex.Pattern.compile(joinPattern);
        matcher = pattern.matcher(lowerSql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果找不到显式的别名，尝试找形如 table_name t1 的简短别名
        int tableIndex = lowerSql.indexOf(lowerTableName);
        if (tableIndex >= 0) {
            int afterTableIndex = tableIndex + lowerTableName.length();
            if (afterTableIndex < lowerSql.length()) {
                String afterTable = lowerSql.substring(afterTableIndex).trim();
                String[] parts = afterTable.split("\\s+");
                if (parts.length > 0 && parts[0].matches("[a-z0-9_]+")) {
                    return parts[0];
                }
            }
        }
        
        return null;
    }
    
    private StringBuilder replaceColumnWithMask(StringBuilder sql, String columnName, String mask) {
        try {
            // 使用固定字符串替换
            String pattern = "\\b" + columnName + "\\b";
            String replacement = "'" + mask + "'";
            
            System.out.println("完全遮盖替换前SQL: " + sql);
            System.out.println("模式: " + pattern);
            System.out.println("替换值: " + replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            System.out.println("完全遮盖替换后SQL: " + result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            System.err.println("完全遮盖替换时出错: " + e.getMessage());
            e.printStackTrace();
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithPartialMask(StringBuilder sql, String columnName) {
        try {
            // 保留前3位和后4位，中间用*代替，使用MySQL支持的语法
            String pattern = "\\b" + columnName + "\\b";
            // 使用MySQL函数语法，去掉表限定符
            String replacement = "CONCAT(SUBSTRING(" + columnName + ", 1, 3), '****', SUBSTRING(" + columnName + ", -4))";
            
            System.out.println("替换前SQL: " + sql);
            System.out.println("模式: " + pattern);
            System.out.println("替换值: " + replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            System.out.println("替换后SQL: " + result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            System.err.println("部分遮盖替换时出错: " + e.getMessage());
            e.printStackTrace();
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithValue(StringBuilder sql, String columnName, String value) {
        try {
            // 使用固定值替换
            String pattern = "\\b" + columnName + "\\b";
            String replacement = "'" + value + "'";
            
            System.out.println("替换值替换前SQL: " + sql);
            System.out.println("模式: " + pattern);
            System.out.println("替换值: " + replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            System.out.println("替换值替换后SQL: " + result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            System.err.println("替换值替换时出错: " + e.getMessage());
            e.printStackTrace();
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithHash(StringBuilder sql, String columnName) {
        try {
            // 使用MD5哈希替换
            String pattern = "\\b" + columnName + "\\b";
            String replacement = "MD5(" + columnName + ")";
            
            System.out.println("哈希替换前SQL: " + sql);
            System.out.println("模式: " + pattern);
            System.out.println("替换值: " + replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            System.out.println("哈希替换后SQL: " + result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            System.err.println("哈希替换时出错: " + e.getMessage());
            e.printStackTrace();
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithRandom(StringBuilder sql, String columnName) {
        try {
            // 使用随机值替换
            String pattern = "\\b" + columnName + "\\b";
            String replacement = "CONCAT('RAND_', FLOOR(RAND() * 1000))";
            
            System.out.println("随机化替换前SQL: " + sql);
            System.out.println("模式: " + pattern);
            System.out.println("替换值: " + replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            System.out.println("随机化替换后SQL: " + result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            System.err.println("随机化替换时出错: " + e.getMessage());
            e.printStackTrace();
            return sql;
        }
    }
    
    public void updateMaskingRules(String tableName, List<MaskingRuleEntity> rules) {
        maskingRules.put(tableName, rules);
    }
    
    public void clearMaskingRules() {
        maskingRules.clear();
    }
    
    /**
     * 输出当前所有的脱敏规则
     */
    public void printAllMaskingRules() {
        System.out.println("==================== 当前所有脱敏规则 ====================");
        if (maskingRules.isEmpty()) {
            System.out.println("没有配置任何脱敏规则");
        } else {
            System.out.println("共有 " + maskingRules.size() + " 个表配置了脱敏规则");
            for (Map.Entry<String, List<MaskingRuleEntity>> entry : maskingRules.entrySet()) {
                String tableName = entry.getKey();
                List<MaskingRuleEntity> rules = entry.getValue();
                System.out.println("表 '" + tableName + "' 配置了 " + rules.size() + " 条规则:");
                for (MaskingRuleEntity rule : rules) {
                    System.out.println("    列: " + rule.getColumnName() + 
                                       ", 脱敏类型: " + rule.getMaskingType() + 
                                       ", 活动状态: " + (rule.isActive() ? "启用" : "禁用"));
                }
            }
        }
        System.out.println("===========================================================");
    }
} 