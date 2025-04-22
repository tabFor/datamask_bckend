package com.example.service.impl;

import com.example.service.MaskedDataService;
import com.example.utils.RedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 脱敏数据服务实现类
 */
@Service
public class MaskedDataServiceImpl implements MaskedDataService {

    private static final Logger logger = LoggerFactory.getLogger(MaskedDataServiceImpl.class);
    private static final String MASKED_DATA_DIR = "masked_data";
    private static final String MASKED_TABLE_PREFIX = "masked_";
    private static final String MASKED_TABLES_CACHE_KEY = "masked_tables:list";
    private static final String MASKED_TABLE_CACHE_KEY_PREFIX = "masked_table:";
    private static final long CACHE_EXPIRE_HOURS = 1;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> listMaskedFiles() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // 从脱敏数据目录获取所有文件
            Path maskedDataPath = Paths.get(MASKED_DATA_DIR);
            if (!Files.exists(maskedDataPath)) {
                logger.warn("脱敏数据目录不存在: {}", MASKED_DATA_DIR);
                return result;
            }
            
            // 递归遍历目录查找所有文件
            try (Stream<Path> paths = Files.walk(maskedDataPath)) {
                paths.filter(Files::isRegularFile)
                     .forEach(path -> {
                         Map<String, Object> fileInfo = new HashMap<>();
                         fileInfo.put("path", path.toString().replace("\\", "/"));
                         fileInfo.put("name", path.getFileName().toString());
                         fileInfo.put("size", getReadableFileSize(path));
                         fileInfo.put("sizeBytes", getFileSizeBytes(path));
                         fileInfo.put("lastModified", new Date(path.toFile().lastModified()));
                         fileInfo.put("type", getFileType(path.toString()));
                         
                         result.add(fileInfo);
                     });
            }
            
            logger.info("找到 {} 个脱敏数据文件", result.size());
            return result;
        } catch (Exception e) {
            logger.error("获取脱敏数据文件列表失败", e);
            throw new RuntimeException("获取脱敏数据文件列表失败", e);
        }
    }

    @Override
    public Map<String, Object> previewMaskedData(String filePath, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.error("文件不存在: {}", filePath);
                throw new FileNotFoundException("文件不存在: " + filePath);
            }
            
            // 确保文件在脱敏数据目录内
            if (!path.toAbsolutePath().toString().replace("\\", "/").contains(MASKED_DATA_DIR)) {
                logger.error("尝试访问脱敏数据目录外的文件: {}", filePath);
                throw new SecurityException("无法访问脱敏数据目录外的文件");
            }
            
            String fileType = getFileType(filePath);
            logger.info("预览文件: {}, 类型: {}, 页码: {}, 每页大小: {}", filePath, fileType, page, size);
            
            switch (fileType) {
                case "csv":
                    data = previewCsvFile(path, page, size, result);
                    break;
                case "json":
                    data = previewJsonFile(path, page, size, result);
                    break;
                default:
                    logger.error("不支持预览的文件类型: {}", fileType);
                    throw new UnsupportedOperationException("不支持的文件类型: " + fileType);
            }
            
            result.put("data", data);
            result.put("filePath", filePath);
            result.put("fileName", path.getFileName().toString());
            result.put("fileType", fileType);
            
            return result;
        } catch (Exception e) {
            logger.error("预览脱敏数据失败: {}", filePath, e);
            throw new RuntimeException("预览脱敏数据失败", e);
        }
    }

    @Override
    public Resource getMaskedFileAsResource(String filePath) {
        try {
            // 规范化文件路径
            Path normalizedPath = Paths.get(filePath).normalize();
            Path maskedDataPath = Paths.get(MASKED_DATA_DIR).toAbsolutePath().normalize();
            
            logger.info("尝试下载文件 - 请求路径: {}, 规范化路径: {}, 脱敏数据目录: {}", 
                filePath, normalizedPath, maskedDataPath);
            
            if (!Files.exists(normalizedPath)) {
                logger.error("文件不存在 - 路径: {}", normalizedPath);
                throw new FileNotFoundException("文件不存在: " + filePath);
            }
            
            // 确保文件在脱敏数据目录内
            String normalizedPathStr = normalizedPath.toString().replace("\\", "/");
            String maskedDataPathStr = maskedDataPath.toString().replace("\\", "/");
            
            // 检查文件路径是否以脱敏数据目录开头
            if (!normalizedPathStr.startsWith(maskedDataPathStr) && 
                !normalizedPathStr.startsWith(MASKED_DATA_DIR)) {
                logger.error("安全违规 - 尝试访问脱敏数据目录外的文件: {}", normalizedPathStr);
                throw new SecurityException("无法访问脱敏数据目录外的文件");
            }
            
            logger.info("成功获取文件资源: {}", normalizedPath);
            Resource resource = new FileSystemResource(normalizedPath.toFile());
            
            if (!resource.exists()) {
                logger.error("文件资源不存在: {}", normalizedPath);
                throw new FileNotFoundException("文件资源不存在: " + filePath);
            }
            
            return resource;
        } catch (Exception e) {
            logger.error("获取脱敏数据文件资源失败: {}", filePath, e);
            throw new RuntimeException("获取脱敏数据文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> queryMaskedTable(String tableName, int page, int size, String conditionsJson) {
        try {
            // 构建缓存key
            String cacheKey = MASKED_TABLE_CACHE_KEY_PREFIX + tableName + ":" + page + ":" + size + ":" + conditionsJson;
            
            // 尝试从缓存获取
            Object cachedResult = redisUtils.get(cacheKey);
            if (cachedResult != null) {
                return (Map<String, Object>) cachedResult;
            }
            
            // 如果缓存中没有，从数据库获取
            Map<String, Object> result = new HashMap<>();
            
            // 构建查询SQL
            StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);
            List<Object> params = new ArrayList<>();
            
            // 添加查询条件
            if (conditionsJson != null && !conditionsJson.trim().isEmpty()) {
                try {
                    Map<String, Object> conditions = objectMapper.readValue(conditionsJson, Map.class);
                    if (!conditions.isEmpty()) {
                        sql.append(" WHERE ");
                        conditions.forEach((key, value) -> {
                            if (sql.toString().endsWith("WHERE ")) {
                                sql.append(key).append(" = ?");
                            } else {
                                sql.append(" AND ").append(key).append(" = ?");
                            }
                            params.add(value);
                        });
                    }
                } catch (Exception e) {
                    logger.error("解析查询条件失败: {}", conditionsJson, e);
                }
            }
            
            // 添加分页
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add(page * size);
            
            // 执行查询
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
            // 获取总记录数
            String countSql = "SELECT COUNT(*) FROM " + tableName;
            int total = jdbcTemplate.queryForObject(countSql, Integer.class);
            
            result.put("data", data);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));
            result.put("tableName", tableName);
            
            // 获取表结构信息
            List<Map<String, Object>> columns = getTableColumns(tableName);
            result.put("columns", columns);
            
            // 存入缓存
            redisUtils.set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            return result;
        } catch (Exception e) {
            logger.error("查询脱敏数据表失败: {}", tableName, e);
            throw new RuntimeException("查询脱敏数据表失败", e);
        }
    }

    @Override
    public List<String> listMaskedTables() {
        try {
            // 尝试从缓存获取
            logger.info("尝试从缓存获取脱敏数据表列表");
            Object cachedTables = redisUtils.get(MASKED_TABLES_CACHE_KEY);
            
            if (cachedTables != null) {
                try {
                    // 尝试转换为列表类型
                    if (cachedTables instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> tables = (List<String>) cachedTables;
                        logger.info("从缓存获取到 {} 个脱敏数据表", tables.size());
                        return tables;
                    } else {
                        logger.warn("缓存中的数据类型不是List，而是: {}", cachedTables.getClass().getName());
                        // 清除缓存中错误的数据
                        redisUtils.delete(MASKED_TABLES_CACHE_KEY);
                    }
                } catch (Exception e) {
                    logger.warn("从缓存获取数据时发生类型转换错误: {}", e.getMessage());
                    // 清除缓存中错误的数据
                    redisUtils.delete(MASKED_TABLES_CACHE_KEY);
                }
            }
            
            // 如果缓存中没有或类型不正确，从数据库获取
            return loadMaskedTablesFromDatabase();
            
        } catch (Exception e) {
            logger.error("获取脱敏数据表列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取脱敏数据表列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从数据库加载脱敏表列表
     */
    private List<String> loadMaskedTablesFromDatabase() {
        logger.info("从数据库加载脱敏数据表列表");
        String dbName = "datamask"; // 数据库名称
        List<String> allTables;
        
        try {
            // 尝试使用SHOW TABLES查询
            String sql = "SHOW TABLES FROM " + dbName;
            logger.info("执行SQL: {}", sql);
            allTables = jdbcTemplate.queryForList(sql, String.class);
            logger.info("从数据库获取到所有表: {}", allTables);
        } catch (Exception e) {
            // 如果SHOW TABLES查询失败，尝试使用information_schema查询
            logger.warn("使用SHOW TABLES查询失败: {}，尝试使用information_schema查询", e.getMessage());
            String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
            logger.info("执行SQL: {}", sql);
            allTables = jdbcTemplate.queryForList(sql, String.class, dbName);
            logger.info("从information_schema获取到所有表: {}", allTables);
        }
        
        // 过滤只返回脱敏相关的表
        List<String> allowedTables = new ArrayList<>();
        for (String table : allTables) {
            boolean isAllowed = isAllowedTable(table);
            logger.info("检查表 {} 是否允许: {}", table, isAllowed);
            if (isAllowed) {
                allowedTables.add(table);
            }
        }
        
        // 存入缓存
        if (!allowedTables.isEmpty()) {
            try {
                logger.info("将 {} 个脱敏表列表存入缓存", allowedTables.size());
                redisUtils.set(MASKED_TABLES_CACHE_KEY, allowedTables, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                logger.warn("存储表列表到缓存时发生错误: {}", e.getMessage());
            }
        } else {
            logger.warn("没有找到允许的脱敏表，不缓存空列表");
        }
        
        logger.info("获取到 {} 个可查询的脱敏表: {}", allowedTables.size(), allowedTables);
        return allowedTables;
    }
    
    // 获取表的列信息
    private List<Map<String, Object>> getTableColumns(String tableName) {
        try {
            String cacheKey = "table_columns:" + tableName;
            
            // 尝试从缓存获取
            Object cachedColumns = redisUtils.get(cacheKey);
            if (cachedColumns != null) {
                return (List<Map<String, Object>>) cachedColumns;
            }
            
            // 如果缓存中没有，从数据库获取
            String sql = "SHOW COLUMNS FROM " + tableName;
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql);
            
            // 存入缓存
            redisUtils.set(cacheKey, columns, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            return columns;
        } catch (Exception e) {
            logger.error("获取表结构信息失败: {}", tableName, e);
            return new ArrayList<>();
        }
    }
    
    // 辅助方法：检查表是否允许查询
    private boolean isAllowedTable(String tableName) {
        try {
            // 只允许以脱敏前缀开头的表
            if (tableName.startsWith(MASKED_TABLE_PREFIX)) {
                logger.info("表 {} 是以脱敏前缀开头的表，允许访问", tableName);
                return true;
            }
            
            // 其他表都不允许普通用户访问
            logger.info("表 {} 不是以脱敏前缀开头的表，不允许普通用户访问", tableName);
            return false;
        } catch (Exception e) {
            logger.error("检查表 {} 是否允许查询时发生错误: {}", tableName, e.getMessage(), e);
            // 出现异常时，为安全起见，默认不允许访问
            return false;
        }
    }
    
    // 清除表相关的所有缓存
    @Override
    public void clearTableCache(String tableName) {
        try {
            logger.info("开始清除表 {} 的所有相关缓存", tableName);
            
            // 清除表列表缓存（强制下次重新从数据库获取）
            boolean deleted = redisUtils.delete(MASKED_TABLES_CACHE_KEY);
            logger.info("清除表列表缓存结果: {}", deleted ? "成功" : "缓存不存在或清除失败");
            
            // 清除表结构缓存
            String columnsCacheKey = "table_columns:" + tableName;
            deleted = redisUtils.delete(columnsCacheKey);
            logger.info("清除表结构缓存结果: {}", deleted ? "成功" : "缓存不存在或清除失败");
            
            // 清除表数据缓存（模式匹配删除所有关联缓存）
            String pattern = MASKED_TABLE_CACHE_KEY_PREFIX + tableName + ":*";
            redisUtils.deletePattern(pattern);
            logger.info("已清除表数据缓存模式: {}", pattern);
            
            logger.info("表 {} 的所有相关缓存已清除完毕", tableName);
        } catch (Exception e) {
            logger.error("清除表 {} 缓存时发生错误: {}", tableName, e.getMessage());
            // 不抛出异常，以免影响主要业务流程
        }
    }
    
    // 辅助方法：获取文件类型
    private String getFileType(String filePath) {
        if (filePath.toLowerCase().endsWith(".csv")) {
            return "csv";
        } else if (filePath.toLowerCase().endsWith(".json")) {
            return "json";
        } else {
            return "unknown";
        }
    }
    
    // 辅助方法：获取可读的文件大小
    private String getReadableFileSize(Path path) {
        try {
            long bytes = Files.size(path);
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format("%.2f KB", bytes / 1024.0);
            } else if (bytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", bytes / (1024.0 * 1024));
            } else {
                return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
            }
        } catch (IOException e) {
            logger.error("获取文件大小失败: {}", path, e);
            return "未知";
        }
    }
    
    // 辅助方法：获取文件大小（字节）
    private long getFileSizeBytes(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            logger.error("获取文件大小失败: {}", path, e);
            return 0;
        }
    }
    
    // 辅助方法：预览CSV文件
    private List<Map<String, Object>> previewCsvFile(Path path, int page, int size, Map<String, Object> result) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        long totalLines = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            // 读取CSV头部
            String headerLine = reader.readLine();
            if (headerLine != null) {
                headers = Arrays.asList(headerLine.split(","));
                result.put("headers", headers);
            }
            
            // 跳过之前的行
            for (int i = 0; i < page * size; i++) {
                if (reader.readLine() == null) break;
                totalLines++;
            }
            
            // 读取当前页的数据
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < size) {
                String[] values = line.split(",");
                Map<String, Object> row = new HashMap<>();
                
                for (int i = 0; i < headers.size() && i < values.length; i++) {
                    row.put(headers.get(i), values[i]);
                }
                
                data.add(row);
                count++;
                totalLines++;
            }
            
            // 计算总行数（继续读取以计算总行数）
            while (reader.readLine() != null) {
                totalLines++;
            }
            
            result.put("total", totalLines);
            result.put("page", page);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) totalLines / size));
            
            return data;
        }
    }
    
    // 辅助方法：预览JSON文件
    private List<Map<String, Object>> previewJsonFile(Path path, int page, int size, Map<String, Object> result) throws IOException {
        // 读取整个JSON文件
        String content = new String(Files.readAllBytes(path));
        List<Map<String, Object>> allData;
        
        try {
            // 尝试解析为对象列表
            allData = objectMapper.readValue(content, List.class);
        } catch (Exception e) {
            // 如果不是列表，尝试解析为单个对象
            try {
                Map<String, Object> singleObject = objectMapper.readValue(content, Map.class);
                allData = Collections.singletonList(singleObject);
            } catch (Exception ex) {
                logger.error("JSON文件格式不正确: {}", path, ex);
                throw new IOException("JSON文件格式不正确");
            }
        }
        
        int total = allData.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, total);
        
        // 截取当前页的数据
        List<Map<String, Object>> pageData = startIndex < total ? 
            allData.subList(startIndex, endIndex) : new ArrayList<>();
        
        // 获取表头（从第一条记录）
        if (!allData.isEmpty()) {
            result.put("headers", new ArrayList<>(allData.get(0).keySet()));
        }
        
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) total / size));
        
        return pageData;
    }

    @Override
    public void updateMaskedData(String tableName, Map<String, Object> data) {
        try {
            // 验证表是否在允许查询的列表中
            if (!isAllowedTable(tableName)) {
                logger.error("尝试更新未授权的表: {}", tableName);
                throw new SecurityException("无权更新该表: " + tableName);
            }
            
            // 构建更新SQL
            StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
            List<Object> params = new ArrayList<>();
            
            // 添加更新字段
            data.forEach((key, value) -> {
                if (!"id".equalsIgnoreCase(key)) { // 不更新ID字段
                    if (sql.toString().endsWith("SET ")) {
                        sql.append(key).append(" = ?");
                    } else {
                        sql.append(", ").append(key).append(" = ?");
                    }
                    params.add(value);
                }
            });
            
            // 添加WHERE条件
            if (data.containsKey("id")) {
                sql.append(" WHERE id = ?");
                params.add(data.get("id"));
            } else {
                throw new IllegalArgumentException("更新数据必须包含ID字段");
            }
            
            // 执行更新
            int updatedRows = jdbcTemplate.update(sql.toString(), params.toArray());
            logger.info("更新表 {} 成功，更新了 {} 行数据", tableName, updatedRows);
            
            // 清除相关缓存
            clearTableCache(tableName);
        } catch (Exception e) {
            logger.error("更新脱敏数据失败: {}", tableName, e);
            throw new RuntimeException("更新脱敏数据失败", e);
        }
    }
    
    @Override
    public void deleteMaskedData(String tableName) {
        try {
            // 验证表是否在允许查询的列表中
            if (!isAllowedTable(tableName)) {
                logger.error("尝试删除未授权的表: {}", tableName);
                throw new SecurityException("无权删除该表: " + tableName);
            }
            
            // 执行删除
            String sql = "DELETE FROM " + tableName;
            int deletedRows = jdbcTemplate.update(sql);
            logger.info("删除表 {} 成功，删除了 {} 行数据", tableName, deletedRows);
            
            // 清除相关缓存
            clearTableCache(tableName);
        } catch (Exception e) {
            logger.error("删除脱敏数据失败: {}", tableName, e);
            throw new RuntimeException("删除脱敏数据失败", e);
        }
    }

    @Override
    public void clearAllCaches() {
        try {
            logger.info("开始清除所有脱敏数据相关缓存");
            
            // 清除表列表缓存
            redisUtils.delete(MASKED_TABLES_CACHE_KEY);
            logger.info("已清除脱敏表列表缓存");
            
            // 获取所有表
            String dbName = "datamask";
            List<String> allTables;
            try {
                String sql = "SHOW TABLES FROM " + dbName;
                allTables = jdbcTemplate.queryForList(sql, String.class);
            } catch (Exception e) {
                logger.warn("使用SHOW TABLES查询失败: {}，尝试使用information_schema查询", e.getMessage());
                String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
                allTables = jdbcTemplate.queryForList(sql, String.class, dbName);
            }
            
            // 清除每个表的缓存
            for (String tableName : allTables) {
                if (tableName.startsWith(MASKED_TABLE_PREFIX)) {
                    clearTableCache(tableName);
                    logger.info("已清除表 {} 的缓存", tableName);
                }
            }
            
            // 清除所有masked_table前缀的缓存
            redisUtils.deletePattern(MASKED_TABLE_CACHE_KEY_PREFIX + "*");
            logger.info("已清除所有表数据缓存");
            
            // 清除所有表结构缓存
            redisUtils.deletePattern("table_columns:*");
            logger.info("已清除所有表结构缓存");
            
            logger.info("所有脱敏数据相关缓存已清除完毕");
        } catch (Exception e) {
            logger.error("清除所有缓存时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("清除所有缓存失败: " + e.getMessage(), e);
        }
    }
} 