package com.example.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import com.example.config.MaskingRule;
import com.example.service.DataMaskingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.example.dto.MaskingRuleDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Service
public class DataMaskingServiceImpl implements DataMaskingService {
    private static final Logger logger = LoggerFactory.getLogger(DataMaskingServiceImpl.class);
    
    @Autowired
    private MaskingRule maskingRule;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        System.out.println("MaskingRule bean successfully injected: " + maskingRule);
    }

    // 脱敏用户名，根据配置显示前缀和后缀，其他部分用 **** 替换
    @Override
    public String maskUsername(String username) {
        if (username == null) return null;
        return username.substring(0, Math.min(1, username.length())) + "****";
    }

    @Override
    public String maskPhone(String phone) {
        if (phone == null) return null;
        return phone.length() > 7 ? 
               phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4) : 
               phone;
    }

    @Override
    public String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? 
               email.substring(0, Math.min(2, atIndex)) + "****" + email.substring(atIndex) : 
               email;
    }
    
    @Override
    public String maskIdCard(String idCard) {
        if (idCard == null) return null;
        return idCard.length() > 10 ?
               idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4) :
               idCard;
    }
    
    @Override
    public String maskBankCard(String bankCard) {
        if (bankCard == null) return null;
        return bankCard.length() > 8 ?
               bankCard.substring(0, 4) + "********" + bankCard.substring(bankCard.length() - 4) :
               bankCard;
    }
    
    @Override
    public String maskAddress(String address) {
        if (address == null) return null;
        if (address.length() <= 6) return address;
        
        // 保留前三个字符和后三个字符，中间用****代替
        return address.substring(0, 3) + "****" + address.substring(address.length() - 3);
    }
    
    @Override
    public List<Map<String, Object>> getMaskedData(Map<String, Object> maskingParams) {
        logger.info("开始获取脱敏数据，参数: {}", maskingParams);
        
        try {
            // 从参数中获取规则和表信息
            List<Object> rules = (List<Object>) maskingParams.get("rules");
            @SuppressWarnings("unchecked")
            Map<String, String> columnMappings = (Map<String, String>) maskingParams.get("columnMappings");
            String sourceDatabase = (String) maskingParams.get("sourceDatabase");
            String sourceTables = (String) maskingParams.get("sourceTables");
            
            logger.debug("解析规则: 数量={}, 映射={}", rules != null ? rules.size() : 0, columnMappings);
            logger.debug("规则详情: {}", rules);
            
            // 添加更详细的规则内容输出
            if (rules != null && !rules.isEmpty()) {
                System.out.println("====== 规则对象详细信息 ======");
                for (int i = 0; i < rules.size(); i++) {
                    Object rule = rules.get(i);
                    System.out.println("规则[" + i + "] 类型: " + rule.getClass().getName());
                    System.out.println("规则[" + i + "] 内容: " + rule);
                    // 如果是MaskingRuleDTO，单独输出其关键属性
                    if (rule instanceof MaskingRuleDTO) {
                        MaskingRuleDTO dto = (MaskingRuleDTO) rule;
                        System.out.println("  - 数据库: " + dto.getDatabase());
                        System.out.println("  - 表名: " + dto.getTableName()); 
                        System.out.println("  - 列名: " + dto.getColumnName());
                        System.out.println("  - 脱敏类型: " + dto.getMaskingType());
                        System.out.println("  - 是否激活: " + dto.isActive());
                        System.out.println("  - 前缀长度: " + dto.getPrefixLength());
                        System.out.println("  - 后缀长度: " + dto.getSuffixLength());
                        System.out.println("  - 替换字符: " + dto.getReplacementChar());
                    }
                }
                System.out.println("===========================");
            }
            
            // 获取原始数据
            List<Map<String, Object>> originalData = queryOriginalData(sourceDatabase, sourceTables);
            logger.info("获取到原始数据, 共{}条记录", originalData.size());
            
            if (originalData.isEmpty()) {
                logger.warn("没有获取到原始数据，无法进行脱敏处理");
                return originalData;
            }
            
            // 输出第一条记录的字段信息，用于调试
            if (!originalData.isEmpty()) {
                Map<String, Object> firstRow = originalData.get(0);
                logger.debug("原始数据第一条记录字段: {}", firstRow.keySet());
                // 详细输出列名供对比
                System.out.println("====== 数据表列名详情 ======");
                System.out.println("表: " + sourceTables);
                System.out.println("可用列名: " + firstRow.keySet());
                System.out.println("============================");
            }
            
            // 如果没有规则或规则为空，直接返回原始数据
            if (rules == null || rules.isEmpty()) {
                logger.warn("没有提供脱敏规则，返回原始数据");
                return originalData;
            }
            
            // 应用脱敏规则
            List<Map<String, Object>> maskedData = new ArrayList<>();
            int processedRows = 0;
            int maskedFields = 0;
            
            // 处理原始数据
            for (Map<String, Object> row : originalData) {
                Map<String, Object> maskedRow = new HashMap<>(row);
                boolean rowMasked = false;
                
                // 应用每条脱敏规则
                for (Object ruleObj : rules) {
                    Map<String, Object> rule = convertRuleToMap(ruleObj);
                    if (rule == null) {
                        System.out.println("规则转换失败: " + ruleObj);
                        continue;  // 无法处理的规则类型
                    }
                    
                    // 详细输出转换后的规则Map
                    System.out.println("====== 转换后的规则Map ======");
                    System.out.println("原始规则对象类型: " + ruleObj.getClass().getName());
                    System.out.println("转换后Map内容: " + rule);
                    System.out.println("============================");
                    
                    // 获取规则相关信息
                    String columnName = extractColumnNameFromRule(rule);
                    String maskingType = extractMaskingTypeFromRule(rule);
                    Boolean isActive = extractIsActiveFromRule(rule);
                    
                    System.out.println("提取的列名: " + columnName);
                    System.out.println("提取的脱敏类型: " + maskingType);
                    System.out.println("提取的激活状态: " + isActive);
                    
                    // 检查规则是否激活
                    if (isActive != null && !isActive) {
                        logger.debug("规则未激活，跳过: columnName={}, maskingType={}", columnName, maskingType);
                        System.out.println("规则未激活，跳过此规则");
                        continue;  // 跳过未激活的规则
                    }
                    
                    // 如果列名为null或空，跳过此规则
                    if (columnName == null || columnName.isEmpty()) {
                        logger.debug("规则中列名为空，跳过规则: {}", rule);
                        System.out.println("规则中列名为空，跳过此规则");
                        continue;
                    }
                    
                    // 尝试多种列名格式查找匹配
                    Object value = null;
                    String matchedColumn = null;
                    
                    // 输出尝试匹配的详细信息
                    System.out.println("====== 列名匹配过程 ======");
                    System.out.println("要匹配的列名: " + columnName);
                    
                    // 直接匹配
                    if (row.containsKey(columnName)) {
                        value = row.get(columnName);
                        matchedColumn = columnName;
                        System.out.println("直接匹配成功: " + columnName);
                    } 
                    // 尝试小写匹配
                    else if (row.containsKey(columnName.toLowerCase())) {
                        value = row.get(columnName.toLowerCase());
                        matchedColumn = columnName.toLowerCase();
                        System.out.println("小写匹配成功: " + matchedColumn);
                    } 
                    // 尝试首字母大写匹配
                    else if (row.containsKey(columnName.substring(0, 1).toUpperCase() + columnName.substring(1))) {
                        String capitalizedColumn = columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
                        value = row.get(capitalizedColumn);
                        matchedColumn = capitalizedColumn;
                        System.out.println("首字母大写匹配成功: " + matchedColumn);
                    }
                    // 对所有列名进行不区分大小写的匹配
                    else {
                        System.out.println("尝试不区分大小写匹配...");
                        for (String key : row.keySet()) {
                            System.out.println("  - 检查列: " + key);
                            if (key.equalsIgnoreCase(columnName)) {
                                value = row.get(key);
                                matchedColumn = key;
                                System.out.println("  -> 不区分大小写匹配成功: " + matchedColumn);
                                break;
                            }
                        }
                    }
                    System.out.println("匹配结果: " + (matchedColumn != null ? "成功 - " + matchedColumn : "失败"));
                    System.out.println("===========================");
                    
                    // 检查是否找到匹配的列
                    if (matchedColumn == null) {
                        logger.debug("在数据中找不到对应的列: {}, 可用列: {}", columnName, row.keySet());
                        System.out.println("警告: 在数据中找不到对应的列: " + columnName);
                        continue;
                    }
                    
                    // 只有当值不为null时才进行脱敏
                    if (value != null) {
                        logger.debug("对列{}应用{}脱敏规则，原始值: {}", matchedColumn, maskingType, value);
                        System.out.println("开始脱敏处理 - 列: " + matchedColumn + ", 类型: " + maskingType + ", 原始值: " + value);
                        
                        Object maskedValue = applyMasking(value, maskingType, rule);
                        
                        // 只有当脱敏后的值与原始值不同时才更新
                        if (!value.equals(maskedValue)) {
                            maskedRow.put(matchedColumn, maskedValue);
                            maskedFields++;
                            rowMasked = true;
                            logger.debug("列{}脱敏后的值: {}", matchedColumn, maskedValue);
                            System.out.println("脱敏成功 - 新值: " + maskedValue);
                        } else {
                            logger.debug("列{}的值没有变化，可能是脱敏规则无效", matchedColumn);
                            System.out.println("警告: 脱敏后的值没有变化，可能是脱敏规则无效");
                        }
                    } else {
                        System.out.println("列值为null，跳过脱敏处理");
                    }
                }
                
                maskedData.add(maskedRow);
                if (rowMasked) {
                    processedRows++;
                }
            }
            
            logger.info("完成脱敏处理，共处理{}条记录，{}个字段被脱敏", processedRows, maskedFields);
            System.out.println("====== 脱敏处理统计 ======");
            System.out.println("处理总记录数: " + originalData.size());
            System.out.println("成功脱敏记录数: " + processedRows);
            System.out.println("脱敏字段总数: " + maskedFields);
            System.out.println("===========================");
            
            return maskedData;
        } catch (Exception e) {
            logger.error("脱敏数据处理失败", e);
            throw new RuntimeException("脱敏数据处理失败: " + e.getMessage(), e);
        }
    }
    
    // 将不同类型的规则对象转换为统一的Map格式
    private Map<String, Object> convertRuleToMap(Object ruleObj) {
        try {
            System.out.println("====== 开始转换规则对象 ======");
            System.out.println("规则对象类型: " + ruleObj.getClass().getName());
            
            // 如果是Map类型，直接使用
            if (ruleObj instanceof Map) {
                System.out.println("规则对象是Map类型，直接使用");
                Map<String, Object> ruleMap = (Map<String, Object>) ruleObj;
                // 确保active字段存在且为true
                ruleMap.put("active", true);
                return ruleMap;
            } 
            // 如果是MaskingRuleDTO类型，转换为Map
            else if (ruleObj instanceof MaskingRuleDTO) {
                System.out.println("规则对象是MaskingRuleDTO类型，转换为Map");
                MaskingRuleDTO dto = (MaskingRuleDTO) ruleObj;
                Map<String, Object> rule = new HashMap<>();
                rule.put("database", dto.getDatabase());
                rule.put("tableName", dto.getTableName());
                rule.put("columnName", dto.getColumnName());
                rule.put("maskingType", dto.getMaskingType());
                rule.put("active", true);  // 始终设置为激活状态
                
                // 添加其他属性
                if (dto.getPrefixLength() != null) {
                    rule.put("prefixLength", dto.getPrefixLength());
                }
                if (dto.getSuffixLength() != null) {
                    rule.put("suffixLength", dto.getSuffixLength());
                }
                if (dto.getReplacementChar() != null) {
                    rule.put("replacementChar", dto.getReplacementChar());
                }
                
                System.out.println("转换后的Map: " + rule);
                return rule;
            } 
            // 如果是其他类型，尝试通过反射获取属性
            else {
                logger.debug("尝试通过反射处理未知规则类型: {}", ruleObj.getClass().getName());
                System.out.println("尝试通过反射处理未知规则类型: " + ruleObj.getClass().getName());
                Map<String, Object> rule = new HashMap<>();
                
                for (Method method : ruleObj.getClass().getMethods()) {
                    if (method.getName().startsWith("get") && method.getParameterCount() == 0 && !method.getName().equals("getClass")) {
                        String propertyName = method.getName().substring(3);
                        // 首字母小写
                        propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                        
                        try {
                            Object value = method.invoke(ruleObj);
                            if (value != null) {
                                rule.put(propertyName, value);
                                System.out.println("  提取属性: " + propertyName + " = " + value);
                            }
                        } catch (Exception e) {
                            logger.debug("获取属性值失败: {}", method.getName(), e);
                            System.out.println("  获取属性值失败: " + method.getName() + ", 错误: " + e.getMessage());
                        }
                    }
                }
                
                if (!rule.isEmpty()) {
                    System.out.println("通过反射提取的属性Map: " + rule);
                    return rule;
                }
            }
            
            logger.warn("无法处理的规则类型: {}", ruleObj.getClass().getName());
            System.out.println("警告: 无法处理的规则类型: " + ruleObj.getClass().getName());
            System.out.println("============================");
            return null;
        } catch (Exception e) {
            logger.error("转换规则对象失败", e);
            System.out.println("错误: 转换规则对象失败: " + e.getMessage());
            e.printStackTrace();
            System.out.println("============================");
            return null;
        }
    }
    
    // 从规则中提取列名
    private String extractColumnNameFromRule(Map<String, Object> rule) {
        // 尝试不同的可能键名
        System.out.println("开始提取列名，尝试键: columnName, column, field, name");
        for (String key : Arrays.asList("columnName", "column", "field", "name")) {
            if (rule.containsKey(key) && rule.get(key) != null) {
                System.out.println("找到列名键: " + key + ", 值: " + rule.get(key).toString());
                return rule.get(key).toString();
            }
        }
        System.out.println("未找到列名键!");
        return null;
    }
    
    // 从规则中提取脱敏类型
    private String extractMaskingTypeFromRule(Map<String, Object> rule) {
        // 尝试不同的可能键名
        for (String key : Arrays.asList("maskingType", "type", "pattern")) {
            if (rule.containsKey(key) && rule.get(key) != null) {
                return rule.get(key).toString();
            }
        }
        return null;
    }
    
    // 从规则中提取是否激活
    private Boolean extractIsActiveFromRule(Map<String, Object> rule) {
        // 尝试不同的可能键名
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
    
    @Override
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
            jdbcTemplate.execute(sql);
            
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
            }
            
            logger.info("成功创建表并插入{}条数据到{}", maskedData.size(), targetTable);
            return true;
            
        } catch (Exception e) {
            logger.error("创建表或插入数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建表或插入数据失败", e);
        }
    }
    
    // 辅助方法：根据数据库和表名查询原始数据
    private List<Map<String, Object>> queryOriginalData(String sourceDatabase, String sourceTables) {
        logger.info("开始查询原始数据，数据库: {}，表: {}", sourceDatabase, sourceTables);
        
        try {
            // 解析数据库连接信息
            String dbType = null;
            String host = null;
            String port = null;
            String dbName = null;
            
            // 解析sourceDatabase字符串（格式：类型:XX;主机:XX;端口:XX;数据库:XX）
            if (sourceDatabase != null && !sourceDatabase.isEmpty()) {
                String[] parts = sourceDatabase.split(";");
                for (String part : parts) {
                    String[] keyValue = part.split(":");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        if ("类型".equals(key)) {
                            dbType = value;
                        } else if ("主机".equals(key)) {
                            host = value;
                        } else if ("端口".equals(key)) {
                            port = value;
                        } else if ("数据库".equals(key)) {
                            dbName = value;
                        }
                    }
                }
            }
            
            // 记录解析结果
            logger.info("解析数据库连接信息：类型={}, 主机={}, 端口={}, 数据库={}", dbType, host, port, dbName);
            
            // 解析表名
            String[] tables = sourceTables.split(",");
            if (tables.length == 0) {
                logger.warn("未指定表名");
                return new ArrayList<>();
            }
            
            String tableName = tables[0].trim(); // 暂时只处理第一个表
            logger.info("准备查询表: {}", tableName);
            
            // 构建SQL查询
            String sql = "SELECT * FROM " + tableName;
            logger.debug("执行SQL: {}", sql);
            
            try {
                // 使用JdbcTemplate查询数据
                List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
                logger.info("查询到{}条数据", result.size());
                return result;
            } catch (Exception e) {
                logger.error("执行SQL查询失败: {}", e.getMessage(), e);
                
                // 降级处理：如果查询失败，尝试从文件系统读取示例数据（如果是CSV文件）
                if ("CSV".equalsIgnoreCase(dbType)) {
                    logger.info("尝试从CSV文件读取数据: {}", tableName);
                    return readFromCsvFile(dbName, tableName);
                }
                
                // 如果上述方法都失败，返回模拟数据作为备用方案
                logger.warn("无法获取真实数据，返回模拟数据");
                return generateMockData();
            }
        } catch (Exception e) {
            logger.error("查询原始数据失败: {}", e.getMessage(), e);
            return generateMockData(); // 失败时返回模拟数据
        }
    }
    
    // 从CSV文件读取数据
    private List<Map<String, Object>> readFromCsvFile(String directory, String fileName) {
        List<Map<String, Object>> data = new ArrayList<>();
        String filePath = directory + "/" + fileName;
        
        logger.info("尝试读取CSV文件: {}", filePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // 读取表头
            String headerLine = reader.readLine();
            if (headerLine == null) {
                logger.warn("CSV文件为空");
                return data;
            }
            
            String[] headers = headerLine.split(",");
            // 去除标题中的引号
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].replaceAll("^\"|\"$", "").trim();
            }
            
            // 读取数据行
            String line;
            while ((line = reader.readLine()) != null) {
                // 处理CSV行，注意处理引号内的逗号
                String[] values = parseCSVLine(line);
                
                if (values.length >= headers.length) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        // 去除值中的引号
                        String value = values[i].replaceAll("^\"|\"$", "");
                        row.put(headers[i], value);
                    }
                    data.add(row);
                }
            }
            
            logger.info("从CSV文件读取了{}条数据", data.size());
            return data;
        } catch (IOException e) {
            logger.error("读取CSV文件失败: {}", e.getMessage(), e);
            return generateMockData(); // 失败时返回模拟数据
        }
    }
    
    // 解析CSV行，处理引号内的逗号
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        
        // 添加最后一个字段
        result.add(field.toString());
        
        return result.toArray(new String[0]);
    }
    
    // 生成模拟数据
    private List<Map<String, Object>> generateMockData() {
        logger.warn("使用模拟数据");
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
    
    /**
     * 应用脱敏规则到数据
     */
    private Object applyMasking(Object value, String maskingType, Map<String, Object> rule) {
        if (value == null || maskingType == null) {
            logger.debug("不进行脱敏：值或脱敏类型为空");
            return value;
        }
        
        String strValue = value.toString();
        
        // 记录原始值（截断显示过长的值）
        String logValue = strValue.length() > 20 ? strValue.substring(0, 20) + "..." : strValue;
        logger.debug("应用脱敏规则: 类型={}, 原始值={}, 规则={}", maskingType, logValue, rule);
        
        // 添加更详细的脱敏处理日志
        System.out.println("====== 开始脱敏处理 ======");
        System.out.println("脱敏类型: " + maskingType);
        System.out.println("原始值: " + logValue);
        System.out.println("规则参数: " + rule);
        
        // 打印规则中的关键参数
        System.out.println("规则详情:");
        if (rule.containsKey("prefixLength")) {
            System.out.println("  - 前缀长度: " + rule.get("prefixLength"));
        }
        if (rule.containsKey("suffixLength")) {
            System.out.println("  - 后缀长度: " + rule.get("suffixLength"));
        }
        if (rule.containsKey("replacementChar")) {
            System.out.println("  - 替换字符: " + rule.get("replacementChar"));
        }
        if (rule.containsKey("pattern")) {
            System.out.println("  - 模式: " + rule.get("pattern"));
        }
        
        Object maskedValue = null;
        
        // 忽略大小写处理不同的脱敏类型
        String type = maskingType.toUpperCase();
        
        try {
            switch (type) {
                case "PHONE":
                case "手机号":
                case "电话号码":
                    System.out.println("应用手机号脱敏规则");
                    maskedValue = maskPhone(strValue, rule);
                    break;
                case "ID_CARD":
                case "身份证":
                case "身份证号码":
                    System.out.println("应用身份证号脱敏规则");
                    maskedValue = maskIdCard(strValue, rule);
                    break;
                case "NAME":
                case "姓名":
                    System.out.println("应用姓名脱敏规则");
                    maskedValue = maskName(strValue, rule);
                    break;
                case "EMAIL":
                case "邮箱":
                case "电子邮箱":
                    System.out.println("应用邮箱脱敏规则");
                    maskedValue = maskEmail(strValue, rule);
                    break;
                case "BANK_CARD":
                case "银行卡":
                case "银行卡号":
                    System.out.println("应用银行卡号脱敏规则");
                    maskedValue = maskBankCard(strValue, rule);
                    break;
                case "ADDRESS":
                case "地址":
                    System.out.println("应用地址脱敏规则");
                    maskedValue = maskAddress(strValue, rule);
                    break;
                case "FULL_MASK":
                case "全遮盖":
                case "全部遮盖":
                    System.out.println("应用全遮盖脱敏规则");
                    maskedValue = maskFull(strValue, rule);
                    break;
                default:
                    logger.warn("未知的脱敏类型: {}, 不进行脱敏", maskingType);
                    System.out.println("警告: 未知的脱敏类型: " + maskingType + ", 不进行脱敏");
                    return value;  // 默认不处理
            }
            
            if (maskedValue != null) {
                String logMaskedValue = maskedValue.toString().length() > 20 ? 
                    maskedValue.toString().substring(0, 20) + "..." : maskedValue.toString();
                logger.debug("脱敏完成: 原始值={}, 脱敏后值={}, 类型={}", logValue, logMaskedValue, maskingType);
                
                System.out.println("脱敏结果: " + logMaskedValue);
                
                // 检查脱敏是否实际发生变化
                if (strValue.equals(maskedValue.toString())) {
                    logger.warn("脱敏后的值与原始值相同: 类型={}, 值={}", maskingType, logValue);
                    System.out.println("警告: 脱敏后的值与原始值相同!");
                    System.out.println("  - 原始值: " + strValue);
                    System.out.println("  - 脱敏后值: " + maskedValue.toString());
                }
            } else {
                logger.warn("脱敏结果为null, 返回原始值");
                System.out.println("警告: 脱敏结果为null, 返回原始值");
                return value;
            }
            
            System.out.println("========================");
            return maskedValue;
        } catch (Exception e) {
            logger.error("应用脱敏规则失败: 类型={}, 原始值={}", maskingType, logValue, e);
            System.out.println("错误: 应用脱敏规则失败: " + e.getMessage());
            e.printStackTrace();
            return value; // 出错时返回原始值
        }
    }

    private String maskPhone(String value, Map<String, Object> rule) {
        if (value == null || value.isEmpty()) return value;
        
        try {
            // 获取规则中的参数，如果没有则使用默认值
            int prefixLength = 3;
            int suffixLength = 4;
            String replacementChar = "*";
            
            if (rule.containsKey("prefixLength") && rule.get("prefixLength") != null) {
                if (rule.get("prefixLength") instanceof Number) {
                    prefixLength = ((Number) rule.get("prefixLength")).intValue();
                } else {
                    try {
                        prefixLength = Integer.parseInt(rule.get("prefixLength").toString());
                    } catch (NumberFormatException e) {
                        logger.warn("无法解析prefixLength: {}", rule.get("prefixLength"));
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
                        logger.warn("无法解析suffixLength: {}", rule.get("suffixLength"));
                    }
                }
            }
            
            if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
                replacementChar = rule.get("replacementChar").toString();
            }
            
            // 清理可能的非数字字符
            value = value.replaceAll("[^0-9]", "");
            
            // 如果字符串太短，保持不变
            if (value.length() <= prefixLength + suffixLength) {
                logger.debug("手机号长度不足，无法脱敏: {}", value);
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
            
            logger.debug("手机号脱敏: 前{}位 + {} + 后{}位", prefixLength, replacementChar, suffixLength);
            return masked.toString();
        } catch (Exception e) {
            logger.error("手机号脱敏失败: {}", value, e);
            return value;
        }
    }
    
    private String maskIdCard(String value, Map<String, Object> rule) {
        if (value == null || value.isEmpty()) return value;
        
        try {
            // 获取规则中的参数，如果没有则使用默认值
            int prefixLength = 6;
            int suffixLength = 4;
            String replacementChar = "*";
            
            if (rule.containsKey("prefixLength") && rule.get("prefixLength") != null) {
                if (rule.get("prefixLength") instanceof Number) {
                    prefixLength = ((Number) rule.get("prefixLength")).intValue();
                } else {
                    try {
                        prefixLength = Integer.parseInt(rule.get("prefixLength").toString());
                    } catch (NumberFormatException e) {
                        logger.warn("无法解析prefixLength: {}", rule.get("prefixLength"));
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
                        logger.warn("无法解析suffixLength: {}", rule.get("suffixLength"));
                    }
                }
            }
            
            if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
                replacementChar = rule.get("replacementChar").toString();
            }
            
            // 清理可能的非字母数字字符
            value = value.replaceAll("[^a-zA-Z0-9]", "");
            
            // 如果字符串太短，不处理
            if (value.length() <= prefixLength + suffixLength) {
                logger.debug("身份证号长度不足，无法脱敏: {}", value);
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
            
            logger.debug("身份证脱敏: 前{}位 + {} + 后{}位", prefixLength, replacementChar, suffixLength);
            return masked.toString();
        } catch (Exception e) {
            logger.error("身份证脱敏失败: {}", value, e);
            return value;
        }
    }
    
    private String maskName(String value, Map<String, Object> rule) {
        if (value == null || value.isEmpty()) return value;
        
        try {
            // 获取规则中的参数，如果没有则使用默认值
            int prefixLength = 1;
            String replacementChar = "*";
            
            if (rule.containsKey("prefixLength") && rule.get("prefixLength") != null) {
                if (rule.get("prefixLength") instanceof Number) {
                    prefixLength = ((Number) rule.get("prefixLength")).intValue();
                } else {
                    try {
                        prefixLength = Integer.parseInt(rule.get("prefixLength").toString());
                    } catch (NumberFormatException e) {
                        logger.warn("无法解析prefixLength: {}", rule.get("prefixLength"));
                    }
                }
            }
            
            if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
                replacementChar = rule.get("replacementChar").toString();
            }
            
            // 如果字符串太短，不处理
            if (value.length() <= prefixLength) {
                logger.debug("姓名长度不足，无法脱敏: {}", value);
                return value;
            }
            
            // 执行脱敏
            String prefix = value.substring(0, prefixLength);
            
            // 构建替换字符
            StringBuilder masked = new StringBuilder(prefix);
            for (int i = 0; i < value.length() - prefixLength; i++) {
                masked.append(replacementChar);
            }
            
            logger.debug("姓名脱敏: 前{}位 + {}", prefixLength, replacementChar);
            return masked.toString();
        } catch (Exception e) {
            logger.error("姓名脱敏失败: {}", value, e);
            return value;
        }
    }
    
    private String maskEmail(String value, Map<String, Object> rule) {
        if (value == null || value.isEmpty()) return value;
        
        try {
            // 获取规则中的参数，如果没有则使用默认值
            int prefixLength = 1;
            String replacementChar = "*";
            
            if (rule.containsKey("prefixLength") && rule.get("prefixLength") != null) {
                if (rule.get("prefixLength") instanceof Number) {
                    prefixLength = ((Number) rule.get("prefixLength")).intValue();
                } else {
                    try {
                        prefixLength = Integer.parseInt(rule.get("prefixLength").toString());
                    } catch (NumberFormatException e) {
                        logger.warn("无法解析prefixLength: {}", rule.get("prefixLength"));
                    }
                }
            }
            
            if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
                replacementChar = rule.get("replacementChar").toString();
            }
            
            // 检查是否是有效的邮箱格式
            int atIndex = value.indexOf('@');
            if (atIndex <= 0) {
                logger.debug("无效的邮箱格式，不进行脱敏: {}", value);
                return value; // 无效的邮箱格式
            }
            
            // 执行脱敏
            String username = value.substring(0, atIndex);
            String domain = value.substring(atIndex);
            
            if (username.length() <= prefixLength) {
                logger.debug("邮箱用户名部分长度不足，无法脱敏: {}", username);
                return value; // 如果用户名太短，不处理
            }
            
            String prefix = username.substring(0, prefixLength);
            
            // 构建替换字符
            StringBuilder masked = new StringBuilder(prefix);
            for (int i = 0; i < username.length() - prefixLength; i++) {
                masked.append(replacementChar);
            }
            masked.append(domain);
            
            logger.debug("邮箱脱敏: 保留用户名前{}位 + {}", prefixLength, replacementChar);
            return masked.toString();
        } catch (Exception e) {
            logger.error("邮箱脱敏失败: {}", value, e);
            return value;
        }
    }
    
    private String maskBankCard(String value, Map<String, Object> rule) {
        if (value == null || value.isEmpty()) return value;
        
        try {
            // 获取规则中的参数，如果没有则使用默认值
            int suffixLength = 4;
            String replacementChar = "*";
            
            if (rule.containsKey("suffixLength") && rule.get("suffixLength") != null) {
                if (rule.get("suffixLength") instanceof Number) {
                    suffixLength = ((Number) rule.get("suffixLength")).intValue();
                } else {
                    try {
                        suffixLength = Integer.parseInt(rule.get("suffixLength").toString());
                    } catch (NumberFormatException e) {
                        logger.warn("无法解析suffixLength: {}", rule.get("suffixLength"));
                    }
                }
            }
            
            if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
                replacementChar = rule.get("replacementChar").toString();
            }
            
            // 清理可能的非数字字符
            value = value.replaceAll("[^0-9]", "");
            
            // 如果字符串太短，不处理
            if (value.length() <= suffixLength) {
                logger.debug("银行卡号长度不足，无法脱敏: {}", value);
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
            
            logger.debug("银行卡脱敏: {} + 后{}位", replacementChar, suffixLength);
            return masked.toString();
        } catch (Exception e) {
            logger.error("银行卡脱敏失败: {}", value, e);
            return value;
        }
    }
    
    private String maskAddress(String value, Map<String, Object> rule) {
        if (value == null || value.isEmpty()) return value;
        
        try {
            // 获取规则中的参数，如果没有则使用默认值
            int prefixLength = 10;
            String replacementChar = "*";
            
            if (rule.containsKey("prefixLength") && rule.get("prefixLength") != null) {
                if (rule.get("prefixLength") instanceof Number) {
                    prefixLength = ((Number) rule.get("prefixLength")).intValue();
                } else {
                    try {
                        prefixLength = Integer.parseInt(rule.get("prefixLength").toString());
                    } catch (NumberFormatException e) {
                        logger.warn("无法解析prefixLength: {}", rule.get("prefixLength"));
                    }
                }
            }
            
            if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
                replacementChar = rule.get("replacementChar").toString();
            }
            
            // 如果地址太短，不处理
            if (value.length() <= prefixLength) {
                logger.debug("地址长度不足，无法脱敏: {}", value);
                return value;
            }
            
            // 执行脱敏
            String prefix = value.substring(0, prefixLength);
            
            // 构建替换字符
            StringBuilder masked = new StringBuilder(prefix);
            for (int i = 0; i < value.length() - prefixLength; i++) {
                masked.append(replacementChar);
            }
            
            logger.debug("地址脱敏: 前{}位 + {}", prefixLength, replacementChar);
            return masked.toString();
        } catch (Exception e) {
            logger.error("地址脱敏失败: {}", value, e);
            return value;
        }
    }
    
    private String maskFull(String value, Map<String, Object> rule) {
        if (value == null || value.isEmpty()) return value;
        
        try {
            // 获取规则中的参数，如果没有则使用默认值
            String replacementChar = "*";
            
            if (rule.containsKey("replacementChar") && rule.get("replacementChar") != null) {
                replacementChar = rule.get("replacementChar").toString();
            }
            
            // 全部字符用替代字符替换
            StringBuilder masked = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                masked.append(replacementChar);
            }
            
            logger.debug("全遮盖脱敏: 使用{}替换全部字符", replacementChar);
            return masked.toString();
        } catch (Exception e) {
            logger.error("全遮盖脱敏失败: {}", value, e);
            return value;
        }
    }
}