package com.example.interceptor;

import com.example.model.MaskingRuleEntity;
import com.example.util.LogUtils;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SQLMaskingInterceptor implements StatementInspector {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLMaskingInterceptor.class);
    private final Map<String, List<MaskingRuleEntity>> maskingRules = new ConcurrentHashMap<>();
    
    @Override
    public String inspect(String sql) {
        // 添加调试日志，查看是否拦截到了SQL
        logger.debug("拦截到SQL: {}", sql);
        
        // 检查是否有脱敏规则
        logger.debug("maskingRules.size() = {}, isEmpty = {}", maskingRules.size(), maskingRules.isEmpty());
        
        if (maskingRules.isEmpty()) {
            logger.debug("没有脱敏规则，原样返回SQL");
            return sql;
        }
        
        // 输出当前所有的脱敏规则
        if (logger.isDebugEnabled()) {
            printAllMaskingRules();
        }
        
        // 解析SQL语句，识别表名和列名
        String tableName = extractTableName(sql);
        if (tableName == null) {
            logger.debug("无法提取表名，原样返回SQL");
            return sql;
        }
        
        // 获取该表的脱敏规则
        List<MaskingRuleEntity> rules = maskingRules.get(tableName);
        if (rules == null || rules.isEmpty()) {
            logger.debug("表 {} 没有对应的脱敏规则，原样返回SQL", tableName);
            return sql;
        }
        
        // 根据规则修改SQL
        String modifiedSql = modifySQL(sql, rules);
        
        // 添加日志输出，方便调试
        logger.debug("Original SQL: {}", sql);
        logger.debug("Modified SQL: {}", modifiedSql);
        
        return modifiedSql;
    }
    
    private String extractTableName(String sql) {
        try {
            // 简单的SQL解析，提取表名
            logger.debug("开始从SQL中提取表名: {}", sql);
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
                        logger.debug("从SQL中提取到表名: {}", tablePart);
                        
                        // 不进行驼峰命名转换，直接返回原始表名
                        return tablePart;
                    }
                }
            }
            
            logger.debug("无法从SQL中提取表名");
            return null;
        } catch (Exception e) {
            logger.error("提取表名时出错: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private String modifySQL(String sql, List<MaskingRuleEntity> rules) {
        try {
            // 根据脱敏规则修改SQL
            StringBuilder modifiedSql = new StringBuilder(sql);
            
            logger.debug("开始应用脱敏规则，规则数量: {}", rules.size());
            for (MaskingRuleEntity rule : rules) {
                logger.debug("处理规则: {}.{}, 脱敏类型: {}, 活动状态: {}", 
                            rule.getTableName(), rule.getColumnName(), 
                            rule.getMaskingType(), rule.isActive());
                
                if (!rule.isActive()) {
                    logger.debug("规则未激活，跳过");
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
                            logger.debug("未知的脱敏类型: {}", maskingType);
                            break;
                    }
                    logger.debug("应用脱敏规则后的SQL: {}", modifiedSql);
                } catch (Exception e) {
                    logger.error("应用脱敏规则时发生错误: {}", e.getMessage(), e);
                }
            }
            
            return modifiedSql.toString();
        } catch (Exception e) {
            logger.error("修改SQL出现一般错误: {}", e.getMessage(), e);
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
            
            logger.debug("完全遮盖替换前SQL: {}", sql);
            logger.debug("模式: {}", pattern);
            logger.debug("替换值: {}", replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            logger.debug("完全遮盖替换后SQL: {}", result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            logger.error("完全遮盖替换时出错: {}", e.getMessage(), e);
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithPartialMask(StringBuilder sql, String columnName) {
        try {
            // 保留前3位和后4位，中间用*代替，使用MySQL支持的语法
            String pattern = "\\b" + columnName + "\\b";
            // 使用MySQL函数语法，去掉表限定符
            String replacement = "CONCAT(SUBSTRING(" + columnName + ", 1, 3), '****', SUBSTRING(" + columnName + ", -4))";
            
            logger.debug("替换前SQL: {}", sql);
            logger.debug("模式: {}", pattern);
            logger.debug("替换值: {}", replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            logger.debug("替换后SQL: {}", result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            logger.error("部分遮盖替换时出错: {}", e.getMessage(), e);
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithValue(StringBuilder sql, String columnName, String value) {
        try {
            // 使用固定值替换
            String pattern = "\\b" + columnName + "\\b";
            String replacement = "'" + value + "'";
            
            logger.debug("替换值替换前SQL: {}", sql);
            logger.debug("模式: {}", pattern);
            logger.debug("替换值: {}", replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            logger.debug("替换值替换后SQL: {}", result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            logger.error("替换值替换时出错: {}", e.getMessage(), e);
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithHash(StringBuilder sql, String columnName) {
        try {
            // 使用MD5哈希替换
            String pattern = "\\b" + columnName + "\\b";
            String replacement = "MD5(" + columnName + ")";
            
            logger.debug("哈希替换前SQL: {}", sql);
            logger.debug("模式: {}", pattern);
            logger.debug("替换值: {}", replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            logger.debug("哈希替换后SQL: {}", result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            logger.error("哈希替换时出错: {}", e.getMessage(), e);
            return sql;
        }
    }
    
    private StringBuilder replaceColumnWithRandom(StringBuilder sql, String columnName) {
        try {
            // 使用随机值替换
            String pattern = "\\b" + columnName + "\\b";
            String replacement = "CONCAT('RAND_', FLOOR(RAND() * 1000))";
            
            logger.debug("随机化替换前SQL: {}", sql);
            logger.debug("模式: {}", pattern);
            logger.debug("替换值: {}", replacement);
            
            String result = sql.toString().replaceAll(pattern, replacement);
            logger.debug("随机化替换后SQL: {}", result);
            
            return new StringBuilder(result);
        } catch (Exception e) {
            logger.error("随机化替换时出错: {}", e.getMessage(), e);
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
        if (maskingRules.isEmpty()) {
            logger.debug("当前没有配置任何脱敏规则");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("==================== 当前所有脱敏规则 ====================\n");
        sb.append("共有 ").append(maskingRules.size()).append(" 个表配置了脱敏规则\n");
        
        maskingRules.forEach((tableName, rules) -> {
            sb.append("表 '").append(tableName).append("' 配置了 ").append(rules.size()).append(" 条规则:\n");
            rules.forEach(rule -> {
                sb.append("    列: ").append(rule.getColumnName())
                  .append(", 脱敏类型: ").append(rule.getMaskingType())
                  .append(", 活动状态: ").append(rule.isActive() ? "启用" : "禁用")
                  .append("\n");
            });
        });
        
        sb.append("===========================================================");
        logger.debug(sb.toString());
    }
} 