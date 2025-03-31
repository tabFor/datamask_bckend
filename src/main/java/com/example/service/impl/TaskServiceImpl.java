package com.example.service.impl;

import com.example.dto.MaskingRuleDTO;
import com.example.dto.TaskDTO;
import com.example.model.Task;
import com.example.repository.TaskRepository;
import com.example.service.DesensitizationRuleService;
import com.example.service.StaticDataMaskingService;
import com.example.service.TaskService;
import com.example.utils.RedisUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.lang.StringBuilder;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final String TASK_CACHE_KEY_PREFIX = "task:";
    private static final String TASK_LIST_CACHE_KEY_PREFIX = "task:list:";
    private static final long CACHE_EXPIRE_HOURS = 1;
    private static final String MASKED_TABLE_PREFIX = "masked_";

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DesensitizationRuleService desensitizationRuleService;
    
    @Autowired
    private StaticDataMaskingService staticDataMaskingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    public Page<Task> findTasks(int page, int pageSize, String keyword) {
        String cacheKey = TASK_LIST_CACHE_KEY_PREFIX + page + ":" + pageSize + ":" + (keyword != null ? keyword : "");
        
        // 尝试从缓存获取
        Object cachedResult = redisUtils.get(cacheKey);
        if (cachedResult != null) {
            try {
                // 使用ObjectMapper将缓存数据转换为List<Task>
                List<Task> content = objectMapper.convertValue(cachedResult, 
                    new TypeReference<List<Task>>() {});
                
                // 从数据库获取分页信息
                Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createTime").descending());
                long total = taskRepository.count();
                
                // 创建Page对象
                return new PageImpl<>(content, pageable, total);
            } catch (Exception e) {
                // 如果转换失败，清除缓存并重新查询
                redisUtils.delete(cacheKey);
            }
        }

        // 如果缓存中没有或转换失败，从数据库获取
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("createTime").descending());
        Page<Task> result;
        if (keyword != null && !keyword.trim().isEmpty()) {
            result = taskRepository.findByTaskNameContaining(keyword, pageable);
        } else {
            result = taskRepository.findAll(pageable);
        }

        // 只缓存内容列表
        redisUtils.set(cacheKey, result.getContent(), CACHE_EXPIRE_HOURS, java.util.concurrent.TimeUnit.HOURS);
        return result;
    }

    // 将Map转换为Page对象
    private Page<Task> convertToPage(Map<String, Object> map) {
        // 这里需要实现Map到Page的转换逻辑
        // 由于Page对象比较复杂，建议直接从数据库重新查询
        Pageable pageable = PageRequest.of(
            ((Number) map.get("number")).intValue(),
            ((Number) map.get("size")).intValue(),
            Sort.by("createTime").descending()
        );
        
        String keyword = (String) map.get("keyword");
        if (keyword != null && !keyword.trim().isEmpty()) {
            return taskRepository.findByTaskNameContaining(keyword, pageable);
        }
        return taskRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Task createTask(TaskDTO taskDTO) {
        Task task = new Task();
        task.setTaskName(taskDTO.getTaskName());
        task.setStatus("等待中");
        
        // 设置任务描述
        if (taskDTO.getDescription() != null) {
            task.setTaskDescription(taskDTO.getDescription());
        } else {
            task.setTaskDescription(taskDTO.getTaskDescription());
        }
        
        // 设置优先级
        if (taskDTO.getPriority() != null) {
            task.setPriority(taskDTO.getPriority());
        }
        
        // 构建数据源信息
        StringBuilder sourceDbBuilder = new StringBuilder();
        if (taskDTO.getDbType() != null) {
            sourceDbBuilder.append("类型:").append(taskDTO.getDbType()).append(";");
            sourceDbBuilder.append("主机:").append(taskDTO.getHost()).append(";");
            sourceDbBuilder.append("端口:").append(taskDTO.getPort()).append(";");
            sourceDbBuilder.append("数据库:").append(taskDTO.getDbName());
            task.setSourceDatabase(sourceDbBuilder.toString());
        } else if (taskDTO.getSourceDatabase() != null) {
            task.setSourceDatabase(taskDTO.getSourceDatabase());
        }
        
        // 设置表名
        if (taskDTO.getTableName() != null) {
            task.setSourceTables(taskDTO.getTableName());
        } else if (taskDTO.getSourceTables() != null) {
            task.setSourceTables(taskDTO.getSourceTables());
        }
        
        // 设置输出格式信息
        if (taskDTO.getOutputFormat() != null) {
            task.setOutputFormat(taskDTO.getOutputFormat());
        }
        
        if (taskDTO.getOutputLocation() != null) {
            task.setOutputLocation(taskDTO.getOutputLocation());
        }
        
        if (taskDTO.getOutputTable() != null) {
            task.setOutputTable(taskDTO.getOutputTable());
        }
        
        // 处理脱敏规则
        try {
            if (taskDTO.getMaskingRules() != null && !taskDTO.getMaskingRules().isEmpty()) {
                // 将规则转换为JSON字符串
                String maskingRulesJson = objectMapper.writeValueAsString(taskDTO.getMaskingRules());
                task.setMaskingRules(maskingRulesJson);
            }
            
            // 处理列名与规则ID的映射关系
            if (taskDTO.getColumnMappings() != null && !taskDTO.getColumnMappings().isEmpty()) {
                // 将映射关系转换为JSON字符串
                String columnMappingsJson = objectMapper.writeValueAsString(taskDTO.getColumnMappings());
                task.setColumnMappings(columnMappingsJson);
            }
        } catch (Exception e) {
            throw new RuntimeException("处理脱敏规则或列名映射失败", e);
        }
        
        // 设置创建者
        task.setCreatedBy(taskDTO.getCreatedBy() != null ? taskDTO.getCreatedBy() : "system");
        
        // 设置时间
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // 处理定时执行
        if (taskDTO.getPlanExecuteTime() != null) {
            task.setExecuteTime(taskDTO.getPlanExecuteTime());
        }
        
        Task savedTask = taskRepository.save(task);
        
        // 清除所有任务相关的缓存
        clearTaskListCache();
        
        return savedTask;
    }

    @Override
    public Task findTaskById(Long id) {
        String cacheKey = TASK_CACHE_KEY_PREFIX + id;
        
        // 尝试从缓存获取
        Object cachedTask = redisUtils.get(cacheKey);
        if (cachedTask != null) {
            try {
                // 使用ObjectMapper将缓存数据转换为Task对象
                return objectMapper.convertValue(cachedTask, Task.class);
            } catch (Exception e) {
                // 如果转换失败，清除缓存并重新查询
                redisUtils.delete(cacheKey);
            }
        }

        // 如果缓存中没有或转换失败，从数据库获取
        Task task = taskRepository.findById(id).orElse(null);
        
        // 存入缓存
        if (task != null) {
            redisUtils.set(cacheKey, task, CACHE_EXPIRE_HOURS, java.util.concurrent.TimeUnit.HOURS);
        }
        
        return task;
    }

    @Override
    @Transactional
    public Task executeTask(Long id) {
        logger.info("开始执行任务, 任务ID: {}", id);
        Task task = findTaskById(id);
        
        // 检查任务状态
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        
        if (!"等待中".equals(task.getStatus())) {
            logger.warn("任务状态不允许执行, 任务ID: {}, 当前状态: {}", id, task.getStatus());
            throw new RuntimeException("任务状态不允许执行");
        }
        
        // 更新任务状态为进行中
        task.setStatus("进行中");
        task.setExecuteTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        
        // 清除该任务和列表的缓存
        clearTaskCache(id);
        clearTaskListCache();
        
        // 直接执行脱敏任务
        try {
            logger.info("开始执行脱敏任务, 任务ID: {}", savedTask.getId());
            executeMaskingTask(savedTask);
            return savedTask;
        } catch (Exception e) {
            logger.error("脱敏任务执行异常, 任务ID: {}, 错误信息: {}", savedTask.getId(), e.getMessage(), e);
            handleTaskException(savedTask.getId(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = findTaskById(id);
        
        if ("进行中".equals(task.getStatus())) {
            throw new RuntimeException("任务执行中，无法删除");
        }
        
        taskRepository.deleteById(id);
        
        // 清除该任务和列表的缓存
        clearTaskCache(id);
        clearTaskListCache();
    }

    @Override
    @Transactional
    public Task updateTask(Long id, TaskDTO taskDTO) {
        Task task = findTaskById(id);
        
        if ("进行中".equals(task.getStatus())) {
            throw new RuntimeException("任务执行中，无法更新");
        }
        
        // 更新任务基本信息
        if (taskDTO.getTaskName() != null) {
            task.setTaskName(taskDTO.getTaskName());
        }
        
        // 更新任务描述
        if (taskDTO.getDescription() != null) {
            task.setTaskDescription(taskDTO.getDescription());
        } else if (taskDTO.getTaskDescription() != null) {
            task.setTaskDescription(taskDTO.getTaskDescription());
        }
        
        // 更新优先级
        if (taskDTO.getPriority() != null) {
            task.setPriority(taskDTO.getPriority());
        }
        
        // 更新输出配置
        if (taskDTO.getOutputFormat() != null) {
            task.setOutputFormat(taskDTO.getOutputFormat());
        }
        
        if (taskDTO.getOutputLocation() != null) {
            task.setOutputLocation(taskDTO.getOutputLocation());
        }
        
        if (taskDTO.getOutputTable() != null) {
            task.setOutputTable(taskDTO.getOutputTable());
        }
        
        // 处理脱敏规则
        try {
            if (taskDTO.getMaskingRules() != null && !taskDTO.getMaskingRules().isEmpty()) {
                // 将规则转换为JSON字符串
                String maskingRulesJson = objectMapper.writeValueAsString(taskDTO.getMaskingRules());
                task.setMaskingRules(maskingRulesJson);
            }
            
            // 处理列名与规则ID的映射关系
            if (taskDTO.getColumnMappings() != null && !taskDTO.getColumnMappings().isEmpty()) {
                // 将映射关系转换为JSON字符串
                String columnMappingsJson = objectMapper.writeValueAsString(taskDTO.getColumnMappings());
                task.setColumnMappings(columnMappingsJson);
            }
        } catch (Exception e) {
            throw new RuntimeException("处理脱敏规则或列名映射失败", e);
        }
        
        // 更新时间
        task.setUpdateTime(LocalDateTime.now());
        
        Task updatedTask = taskRepository.save(task);
        
        // 清除该任务和列表的缓存
        clearTaskCache(id);
        clearTaskListCache();
        
        return updatedTask;
    }
    
    // 执行脱敏任务
    private void executeMaskingTask(Task task) {
        try {
            logger.info("执行脱敏任务, 任务ID: {}, 任务名称: {}", task.getId(), task.getTaskName());
            
            // 获取任务的脱敏规则
            String maskingRulesJson = task.getMaskingRules();
            if (maskingRulesJson == null || maskingRulesJson.isEmpty()) {
                logger.warn("任务脱敏规则为空, 任务ID: {}", task.getId());
                throw new RuntimeException("任务脱敏规则为空");
            }
            
            // 获取列名与规则ID的映射关系
            String columnMappingsJson = task.getColumnMappings();
            Map<String, String> columnToRuleMap = new HashMap<>();
            
            if (columnMappingsJson != null && !columnMappingsJson.isEmpty()) {
                try {
                    columnToRuleMap = objectMapper.readValue(columnMappingsJson, new TypeReference<Map<String, String>>() {});
                    logger.debug("成功解析列名映射: {}", columnToRuleMap);
                } catch (Exception e) {
                    logger.warn("解析列名映射失败: {}", e.getMessage());
                    // 解析失败不影响后续处理，使用空映射继续
                }
            } else {
                logger.debug("列名映射为空，将使用规则中的列名");
            }
            
            // 解析脱敏规则
            List<String> ruleIds;
            try {
                // 首先尝试解析为规则ID列表(字符串数组)
                try {
                    ruleIds = objectMapper.readValue(maskingRulesJson, new TypeReference<List<String>>() {});
                    logger.debug("解析为规则ID列表，ID数量: {}", ruleIds.size());
                    
                    // 添加详细输出 - 打印每个规则ID
                    logger.debug("======== 规则ID详情 ========");
                    for (int i = 0; i < ruleIds.size(); i++) {
                        logger.debug("规则ID[{}]': {}", i, ruleIds.get(i));
                    }
                    logger.debug("==========================");
                } catch (Exception e) {
                    // 如果解析为ID列表失败，尝试解析为MaskingRuleDTO列表
                    logger.debug("尝试解析为MaskingRuleDTO列表...");
                    logger.debug("解析失败原因: {}", e.getMessage());
                    logger.debug("原始JSON字符串: {}", maskingRulesJson);
                    
                    List<MaskingRuleDTO> ruleDTOs = objectMapper.readValue(maskingRulesJson, new TypeReference<List<MaskingRuleDTO>>() {});
                    
                    // 确保所有规则都是激活状态
                    for (MaskingRuleDTO dto : ruleDTOs) {
                        dto.setActive(true);
                        logger.debug("设置规则为激活状态: {}, 激活状态: {}", dto.getRuleId(), dto.isActive());
                    }
                    
                    // 添加详细输出 - 打印解析出的DTO对象
                    logger.debug("======== 解析出的MaskingRuleDTO详情 ========");
                    for (int i = 0; i < ruleDTOs.size(); i++) {
                        MaskingRuleDTO dto = ruleDTOs.get(i);
                        logger.debug("规则DTO[{}] {}", i, dto);
                        logger.debug("  - RuleId: '{}'", dto.getRuleId());
                        logger.debug("  - 类型: {}", dto.getMaskingType());
                        logger.debug("  - 列名: {}", dto.getColumnName());
                        logger.debug("  - 数据库: {}", dto.getDatabase());
                        logger.debug("  - 表名: {}", dto.getTableName());
                    }
                    logger.debug("=======================================");
                    
                    // 提取ruleId属性组成ruleIds列表
                    ruleIds = ruleDTOs.stream()
                            .map(MaskingRuleDTO::getRuleId)
                            .filter(id -> id != null && !id.isEmpty())
                            .collect(Collectors.toList());
                    
                    logger.debug("从MaskingRuleDTO列表中提取规则ID，ID数量: {}", ruleIds.size());
                    
                    // 添加详细输出 - 打印提取的规则ID
                    logger.debug("======== 提取的规则ID详情 ========");
                    for (int i = 0; i < ruleIds.size(); i++) {
                        logger.debug("提取规则ID[{}]': {}", i, ruleIds.get(i));
                    }
                    logger.debug("================================");
                    
                    // 如果没有有效的ruleId，则抛出异常
                    if (ruleIds.isEmpty()) {
                        throw new RuntimeException("没有找到有效的规则ID");
                    }
                }
            } catch (Exception e) {
                logger.error("解析脱敏规则失败: {}", e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("无法解析脱敏规则", e);
            }
            
            // 通过StaticDataMaskingService获取规则
            logger.debug("开始通过StaticDataMaskingService获取规则，规则ID数量: {}", ruleIds.size());
            List<Map<String, Object>> maskingRules = staticDataMaskingService.getRulesByIds(ruleIds);
            
            logger.debug("脱敏配置解析完成, 规则数量: {}", maskingRules.size());
            
            // 如果有列名映射，则需要修改规则中的列名
            if (!columnToRuleMap.isEmpty() && !maskingRules.isEmpty()) {
                logger.debug("====== 应用列名映射 ======");
                
                // 创建规则ID到规则对象的映射，方便后续查找
                Map<String, Map<String, Object>> ruleIdToRuleMap = new HashMap<>();
                for (Map<String, Object> rule : maskingRules) {
                    if (rule.containsKey("ruleId")) {
                        ruleIdToRuleMap.put(rule.get("ruleId").toString(), rule);
                    }
                }
                
                // 根据列名映射创建新的规则列表
                List<Map<String, Object>> mappedRules = new ArrayList<>();
                
                for (Map.Entry<String, String> entry : columnToRuleMap.entrySet()) {
                    String columnName = entry.getKey();      // 数据表的列名
                    String ruleId = entry.getValue();        // 对应的规则ID
                    
                    logger.debug("映射列名: {} -> 规则ID: {}", columnName, ruleId);
                    
                    // 从规则映射中获取对应的规则
                    Map<String, Object> rule = ruleIdToRuleMap.get(ruleId);
                    
                    if (rule != null) {
                        // 创建规则副本，避免修改原始规则
                        Map<String, Object> mappedRule = new HashMap<>(rule);
                        // 设置正确的列名
                        mappedRule.put("columnName", columnName);
                        
                        logger.debug("创建映射规则: 列名={}, 规则类型={}", columnName, mappedRule.get("type"));
                        mappedRules.add(mappedRule);
                    } else {
                        logger.warn("警告: 找不到规则ID: {}", ruleId);
                    }
                }
                
                // 如果成功创建了映射规则，则使用映射规则替换原始规则
                if (!mappedRules.isEmpty()) {
                    logger.debug("使用映射后的规则: {}条", mappedRules.size());
                    maskingRules = mappedRules;
                } else {
                    logger.debug("未创建任何映射规则，将使用原始规则");
                }
                
                logger.debug("===========================");
            }
            
            // 添加调试信息，检查最终用于处理的规则
            if (maskingRules.isEmpty()) {
                logger.warn("没有找到任何匹配的脱敏规则，将不会进行数据脱敏处理!");
            } else {
                logger.debug("======== 最终使用的脱敏规则详情 ========");
                for (int i = 0; i < maskingRules.size(); i++) {
                    Map<String, Object> rule = maskingRules.get(i);
                    logger.debug("规则[{}]: {}", i, rule);
                    logger.debug("  - id: {}", rule.get("id"));
                    logger.debug("  - ruleId: {}", rule.get("ruleId"));
                    logger.debug("  - name: {}", rule.get("name"));
                    logger.debug("  - type: {}", rule.get("type"));
                    logger.debug("  - pattern: {}", rule.get("pattern"));
                    logger.debug("  - 列名: {}", staticDataMaskingService.extractColumnName(rule));
                }
                logger.debug("=======================================");
            }
            
            // 获取输出配置
            String outputFormat = task.getOutputFormat();
            if (outputFormat == null || outputFormat.isEmpty()) {
                outputFormat = "CSV"; // 默认使用CSV格式
            }
            
            String outputLocation = task.getOutputLocation();
            String outputTable = task.getOutputTable();
            
            logger.debug("输出配置: 格式={}, 位置={}", 
                outputFormat, 
                (outputFormat.equalsIgnoreCase("CSV") || outputFormat.equalsIgnoreCase("JSON") ? 
                outputLocation : outputTable));
            
            // 解析数据源信息
            String sourceDatabase = task.getSourceDatabase();
            String sourceTables = task.getSourceTables();
            logger.debug("数据源信息: 数据库={}, 表={}", sourceDatabase, sourceTables);
            
            // 记录开始处理
            task.setExecutionLog("开始执行脱敏任务，处理表: " + sourceTables);
            taskRepository.save(task);
            
            // 获取原始数据
            List<Map<String, Object>> originalData = staticDataMaskingService.queryOriginalData(sourceDatabase, sourceTables);
            
            // 使用静态脱敏服务处理数据
            logger.debug("获取脱敏处理后的数据...");
            List<Map<String, Object>> maskedData = staticDataMaskingService.processMaskedData(originalData, maskingRules);
            logger.debug("成功获取脱敏后数据, 共 {} 条记录", maskedData.size());
            
            task.setExecutionLog(task.getExecutionLog() + "\n脱敏处理完成，准备输出数据");
            taskRepository.save(task);
            
            // 根据输出格式处理
            if ("CSV".equalsIgnoreCase(outputFormat)) {
                try {
                    logger.debug("输出格式: CSV");
                    // 创建输出目录
                    String outputDir = outputLocation;
                    if (outputDir == null || outputDir.isEmpty()) {
                        // 默认输出到当前目录下的masked_data文件夹
                        outputDir = "masked_data/" + task.getId();
                    }
                    
                    java.io.File directory = new java.io.File(outputDir);
                    if (!directory.exists()) {
                        if (directory.mkdirs()) {
                            logger.debug("成功创建目录: {}", outputDir);
                        } else {
                            throw new RuntimeException("无法创建目录: " + outputDir);
                        }
                    }
                    
                    // 生成CSV文件名（使用表名和时间戳）
                    String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
                    String fileName = sourceTables.replace(",", "_") + "_" + timestamp + ".csv";
                    String filePath = outputDir + "/" + fileName;
                    
                    // 将数据写入CSV文件
                    logger.debug("开始写入CSV文件: {}", filePath);
                    taskRepository.save(task);
                    
                    try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                        // 写入表头
                        if (!maskedData.isEmpty()) {
                            Map<String, Object> firstRow = maskedData.get(0);
                            String header = String.join(",", firstRow.keySet());
                            writer.write(header + "\n");
                            
                            // 写入数据行
                            for (Map<String, Object> row : maskedData) {
                                String line = row.values().stream()
                                    .map(val -> val == null ? "" : val.toString().replace(",", "\\,"))
                                    .map(val -> "\"" + val + "\"")
                                    .reduce((a, b) -> a + "," + b)
                                    .orElse("");
                                writer.write(line + "\n");
                            }
                        }
                    }
                    
                    task.setExecutionLog(task.getExecutionLog() + "\n成功生成CSV文件: " + filePath);
                    logger.debug("成功生成CSV文件: {}", filePath);
                } catch (Exception e) {
                    logger.error("生成CSV文件失败: {}", e.getMessage());
                    e.printStackTrace();
                    task.setExecutionLog(task.getExecutionLog() + "\nCSV文件生成失败: " + e.getMessage());
                    throw new RuntimeException("生成CSV文件失败", e);
                }
            } else if ("JSON".equalsIgnoreCase(outputFormat)) {
                try {
                    logger.debug("输出格式: JSON");
                    // 创建输出目录
                    String outputDir = outputLocation;
                    if (outputDir == null || outputDir.isEmpty()) {
                        // 默认输出到当前目录下的masked_data文件夹
                        outputDir = "masked_data/" + task.getId();
                    }
                    
                    java.io.File directory = new java.io.File(outputDir);
                    if (!directory.exists()) {
                        if (directory.mkdirs()) {
                            logger.debug("成功创建目录: {}", outputDir);
                        } else {
                            throw new RuntimeException("无法创建目录: " + outputDir);
                        }
                    }
                    
                    // 生成JSON文件名（使用表名和时间戳）
                    String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
                    String fileName = sourceTables.replace(",", "_") + "_" + timestamp + ".json";
                    String filePath = outputDir + "/" + fileName;
                    
                    // 将数据写入JSON文件
                    logger.debug("开始写入JSON文件: {}", filePath);
                    taskRepository.save(task);
                    
                    try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                        // 使用Jackson将数据转换为JSON格式
                        String jsonData = objectMapper.writeValueAsString(maskedData);
                        writer.write(jsonData);
                    }
                    
                    task.setExecutionLog(task.getExecutionLog() + "\n成功生成JSON文件: " + filePath);
                    logger.debug("成功生成JSON文件: {}", filePath);
                } catch (Exception e) {
                    logger.error("生成JSON文件失败: {}", e.getMessage());
                    e.printStackTrace();
                    task.setExecutionLog(task.getExecutionLog() + "\nJSON文件生成失败: " + e.getMessage());
                    throw new RuntimeException("生成JSON文件失败", e);
                }
            } else if ("DATABASE".equalsIgnoreCase(outputFormat) || "original".equalsIgnoreCase(outputFormat)) {
                try {
                    logger.debug("输出格式: {}", outputFormat);
                    // 获取目标表名
                    String targetTable = outputTable;
                    if (targetTable == null || targetTable.isEmpty()) {
                        // 如果未指定目标表，则生成一个默认名称
                        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
                        targetTable = MASKED_TABLE_PREFIX + sourceTables.replace(",", "_") + "_" + timestamp;
                    } else if (!targetTable.startsWith(MASKED_TABLE_PREFIX)) {
                        // 如果用户指定了表名但没有前缀，则添加前缀
                        targetTable = MASKED_TABLE_PREFIX + targetTable;
                    }
                    
                    logger.debug("开始创建目标表并写入数据: {}", targetTable);
                    taskRepository.save(task);
                    
                    // 判断是否为同库同表场景（覆盖原表）
                    boolean isSameTable = sourceTables.equals(targetTable);
                    // 判断是否为同库不同表场景
                    boolean isSameDatabase = true; // 目前默认为同库
                    
                    boolean success;
                    if (isSameTable) {
                        // 同库同表场景：备份原表并覆盖
                        logger.debug("检测到同库同表脱敏场景（覆盖原表）");
                        String backupTableName = sourceTables + "_backup_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
                        logger.debug("备份原表到: {}", backupTableName);
                        
                        // 1. 备份原表
                        String backupSql = "CREATE TABLE " + backupTableName + " AS SELECT * FROM " + sourceTables;
                        jdbcTemplate.execute(backupSql);
                        
                        // 2. 清空原表
                        String truncateSql = "TRUNCATE TABLE " + sourceTables;
                        jdbcTemplate.execute(truncateSql);
                        
                        // 3. 将脱敏数据插入原表
                        success = insertDataToExistingTable(sourceTables, maskedData);
                        
                        task.setExecutionLog(task.getExecutionLog() + "\n原表已备份为: " + backupTableName);
                    } else if (isSameDatabase) {
                        // 同库不同表场景：创建新表并插入数据
                        logger.debug("检测到同库不同表脱敏场景");
                        success = staticDataMaskingService.createTableAndInsertData(targetTable, maskedData);
                    } else {
                        // 跨库场景：创建新表并插入数据（实际实现可能需要更多的配置）
                        logger.debug("检测到跨库脱敏场景");
                        success = staticDataMaskingService.createTableAndInsertData(targetTable, maskedData);
                    }
                    
                    if (success) {
                        task.setExecutionLog(task.getExecutionLog() + "\n成功创建并写入数据库表: " + targetTable);
                        logger.debug("成功创建并写入数据库表: {}", targetTable);
                    } else {
                        throw new RuntimeException("写入数据库表失败");
                    }
                } catch (Exception e) {
                    logger.error("写入数据库表失败: {}", e.getMessage());
                    e.printStackTrace();
                    task.setExecutionLog(task.getExecutionLog() + "\n写入数据库表失败: " + e.getMessage());
                    throw new RuntimeException("写入数据库表失败", e);
                }
            }
            
            // 完成处理
            task.setStatus("已完成");
            task.setUpdateTime(LocalDateTime.now());
            task.setExecutionLog(task.getExecutionLog() + "\n脱敏任务完成");

            // 添加详细的完成信息
            logger.debug("====== 脱敏任务执行结果 ======");
            logger.debug("任务ID: {}", task.getId());
            logger.debug("任务名称: {}", task.getTaskName());
            logger.debug("原始数据量: {}", (originalData != null ? originalData.size() : 0));
            logger.debug("脱敏数据量: {}", (maskedData != null ? maskedData.size() : 0));
            logger.debug("规则数量: {}", (maskingRules != null ? maskingRules.size() : 0));
            
            // 计算脱敏数据与原始数据的差异统计
            if (originalData != null && !originalData.isEmpty() && maskedData != null && !maskedData.isEmpty() && 
                originalData.size() == maskedData.size()) {
                    
                int modifiedRows = 0;
                int modifiedFields = 0;
                
                for (int i = 0; i < originalData.size(); i++) {
                    Map<String, Object> origRow = originalData.get(i);
                    Map<String, Object> maskRow = maskedData.get(i);
                    
                    boolean rowModified = false;
                    for (String key : origRow.keySet()) {
                        Object origValue = origRow.get(key);
                        Object maskValue = maskRow.get(key);
                        
                        if (origValue != null && maskValue != null && !origValue.equals(maskValue)) {
                            modifiedFields++;
                            rowModified = true;
                        }
                    }
                    
                    if (rowModified) {
                        modifiedRows++;
                    }
                }
                
                logger.debug("脱敏统计:");
                logger.debug("  - 被修改的记录数: {}", modifiedRows);
                logger.debug("  - 被修改的字段数: {}", modifiedFields);
                
                // 输出示例脱敏效果
                if (modifiedRows > 0 && !originalData.isEmpty()) {
                    logger.debug("\n脱敏效果示例 (第一条修改记录):");
                    boolean foundExample = false;
                    
                    for (int i = 0; i < Math.min(originalData.size(), 5) && !foundExample; i++) {
                        Map<String, Object> origRow = originalData.get(i);
                        Map<String, Object> maskRow = maskedData.get(i);
                        boolean hasChanges = false;
                        
                        for (String key : origRow.keySet()) {
                            Object origValue = origRow.get(key);
                            Object maskValue = maskRow.get(key);
                            
                            if (origValue != null && maskValue != null && !origValue.equals(maskValue)) {
                                if (!hasChanges) {
                                    hasChanges = true;
                                    logger.debug("记录 #{}:", i+1);
                                }
                                logger.debug("  - 字段: {}", key);
                                logger.debug("    原始值: {}", origValue);
                                logger.debug("    脱敏后: {}", maskValue);
                            }
                        }
                        
                        if (hasChanges) {
                            foundExample = true;
                        }
                    }
                    
                    if (!foundExample) {
                        logger.debug("未找到示例记录");
                    }
                }
            } else {
                logger.debug("注意: 脱敏前后数据量不一致或为空，无法计算差异统计");
            }
            
            logger.debug("==================================");
            
            // 保存任务状态
            taskRepository.save(task);
        } catch (Exception e) {
            logger.error("脱敏任务执行失败: {}, 任务ID: {}", e.getMessage(), task.getId(), e);
            e.printStackTrace();
            throw new RuntimeException("脱敏任务执行失败: " + e.getMessage(), e);
        }
    }
    
    // 处理任务执行异常
    private void handleTaskException(Long taskId, Exception e) {
        try {
            logger.error("处理任务异常, 任务ID: {}, 错误信息: {}", taskId, e.getMessage());
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                task.setStatus("失败");
                task.setExecutionLog("任务执行失败: " + e.getMessage());
                task.setUpdateTime(LocalDateTime.now());
                taskRepository.save(task);
                logger.info("任务状态已更新为'失败', 任务ID: {}", taskId);
            } else {
                logger.info("找不到任务记录, 无法更新状态, 任务ID: {}", taskId);
            }
        } catch (Exception ex) {
            // 记录日志
            logger.error("处理任务异常失败: {}, 任务ID: {}", ex.getMessage(), taskId, ex);
            ex.printStackTrace();
        }
    }
    
    /**
     * 将脱敏数据插入已存在的表中
     */
    private boolean insertDataToExistingTable(String targetTable, List<Map<String, Object>> maskedData) {
        try {
            if (maskedData == null || maskedData.isEmpty()) {
                logger.debug("没有可插入的数据");
                return false;
            }
            
            // 获取第一行数据的列名
            Map<String, Object> firstRow = maskedData.get(0);
            
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
            int count = 0;
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
                count++;
            }
            
            logger.debug("成功插入{}条数据到{}", count, targetTable);
            return true;
            
        } catch (Exception e) {
            logger.error("插入数据失败: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("插入数据失败", e);
        }
    }

    // 清除任务缓存
    private void clearTaskCache(Long taskId) {
        redisUtils.delete(TASK_CACHE_KEY_PREFIX + taskId);
    }

    // 清除任务列表缓存
    private void clearTaskListCache() {
        // 清除所有任务列表相关的缓存
        redisUtils.deletePattern(TASK_LIST_CACHE_KEY_PREFIX + "*");
    }

    @Override
    public Task getTaskById(Long id) {
        return findTaskById(id);
    }

    @Override
    @Transactional
    public Task updateTaskStatus(Long id, String status) {
        Task task = findTaskById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        
        task.setStatus(status);
        task.setUpdateTime(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);
        
        // 清除该任务和列表的缓存
        clearTaskCache(id);
        clearTaskListCache();
        
        return savedTask;
    }
} 