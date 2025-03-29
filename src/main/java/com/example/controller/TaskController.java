package com.example.controller;

import com.example.dto.ApiResponse;
import com.example.dto.TaskDTO;
import com.example.dto.MaskingRuleDTO;
import com.example.model.Task;
import com.example.model.DesensitizationRule;
import com.example.service.TaskService;
import com.example.service.DesensitizationRuleService;
import com.example.service.TaskMessageProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "脱敏任务管理", description = "提供脱敏任务的创建、查询、更新、执行和删除功能，支持批量数据脱敏和数据库实时脱敏")
public class TaskController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private TaskService taskService;

    @Autowired
    private DesensitizationRuleService desensitizationRuleService;

    @Autowired
    private TaskMessageProducer taskMessageProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Operation(
        summary = "获取脱敏任务列表", 
        description = "分页获取脱敏任务列表，支持按任务名称或描述进行关键词搜索。返回任务ID、名称、状态、创建时间等基本信息。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取任务列表", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping
    public ApiResponse<Map<String, Object>> getTasks(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "搜索关键词，可搜索任务名称或描述") @RequestParam(required = false) String keyword) {
        Page<Task> taskPage = taskService.findTasks(page, pageSize, keyword);
        
        // 转换结果为前端期望的格式
        List<Map<String, Object>> tasks = taskPage.getContent().stream()
                .map(this::convertTaskToMap)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("tasks", tasks);
        response.put("total", taskPage.getTotalElements());
        
        return ApiResponse.success(response);
    }

    @Operation(
        summary = "获取脱敏任务详情", 
        description = "根据任务ID获取脱敏任务的详细信息，包括任务配置、执行状态、脱敏规则映射和输出配置等"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取任务详情"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定ID的任务不存在")
    })
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getTask(
            @Parameter(description = "脱敏任务ID") @PathVariable Long id) {
        try {
            Task task = taskService.findTaskById(id);
            return ApiResponse.success(convertTaskToMap(task));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(
        summary = "创建脱敏任务", 
        description = "创建新的数据脱敏任务，需指定数据源、目标表/文件、脱敏规则映射等信息。创建后的任务需手动执行才会开始脱敏流程。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "任务创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误或任务配置无效")
    })
    @PostMapping
    public ApiResponse<Map<String, Object>> createTask(
            @Parameter(description = "脱敏任务配置信息，包括任务名称、数据源、脱敏规则映射等") 
            @RequestBody TaskDTO taskDTO) {
        try {
            Task task = taskService.createTask(taskDTO);
            return ApiResponse.success(convertTaskToMap(task));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(
        summary = "执行脱敏任务", 
        description = "执行指定ID的脱敏任务，开始数据脱敏处理流程。执行过程为异步，API调用成功后任务将在后台运行，可通过查询任务状态了解执行进度。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "任务开始执行"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "任务状态不允许执行或执行参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定ID的任务不存在")
    })
    @PostMapping("/{id}/execute")
    public ApiResponse<Map<String, Object>> executeTask(
            @Parameter(description = "脱敏任务ID") @PathVariable Long id) {
        try {
            Task task = taskService.getTaskById(id);
            if (task == null) {
                return ApiResponse.error("任务不存在");
            }

            // 检查任务状态
            if (!"等待中".equals(task.getStatus())) {
                return ApiResponse.error("任务状态不允许执行");
            }

            // 发送任务执行消息到队列
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setId(id);
            taskDTO.setTaskName(task.getTaskName());
            taskDTO.setSourceDatabase(task.getSourceDatabase());
            taskDTO.setSourceTables(task.getSourceTables());
            taskDTO.setOutputFormat(task.getOutputFormat());
            taskDTO.setOutputLocation(task.getOutputLocation());
            taskDTO.setOutputTable(task.getOutputTable());
            
            // 设置脱敏规则和列名映射
            try {
                if (task.getMaskingRules() != null) {
                    List<MaskingRuleDTO> maskingRules = objectMapper.readValue(
                        task.getMaskingRules(),
                        new TypeReference<List<MaskingRuleDTO>>() {}
                    );
                    taskDTO.setMaskingRules(maskingRules);
                }
                
                if (task.getColumnMappings() != null) {
                    Map<String, String> columnMappings = objectMapper.readValue(
                        task.getColumnMappings(),
                        new TypeReference<Map<String, String>>() {}
                    );
                    taskDTO.setColumnMappings(columnMappings);
                }
            } catch (Exception e) {
                System.err.println("解析任务规则失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            taskMessageProducer.sendTaskExecutionMessage(id, taskDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "任务已提交到队列，将在后台执行");
            response.put("taskId", id);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("执行任务失败: " + e.getMessage());
        }
    }

    @Operation(
        summary = "更新脱敏任务", 
        description = "更新指定ID的脱敏任务配置。仅允许更新尚未执行的任务，已执行或执行中的任务无法修改。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "任务更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误或任务状态不允许更新"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定ID的任务不存在")
    })
    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateTask(
            @Parameter(description = "脱敏任务ID") @PathVariable Long id, 
            @Parameter(description = "更新后的任务配置信息") @RequestBody TaskDTO taskDTO) {
        try {
            Task task = taskService.updateTask(id, taskDTO);
            return ApiResponse.success(convertTaskToMap(task));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(
        summary = "删除脱敏任务", 
        description = "删除指定ID的脱敏任务。已完成的任务可以删除，但相关的脱敏结果（如输出文件或数据库表）不会自动删除。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "任务删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "任务状态不允许删除"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定ID的任务不存在")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteTask(
            @Parameter(description = "脱敏任务ID") @PathVariable Long id) {
        try {
            taskService.deleteTask(id);
            return ApiResponse.success("任务删除成功");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取创建任务所需的列名-规则映射模板
     */
    @Operation(
        summary = "获取规则映射模板", 
        description = "获取创建脱敏任务时的列名-规则映射建议模板。返回常见的数据库列名和对应的推荐脱敏规则，帮助用户快速配置脱敏映射关系。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取映射模板", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "服务器内部错误"
        )
    })
    @GetMapping("/rules-mapping-template")
    public ApiResponse<Map<String, Object>> getRulesMappingTemplate() {
        try {
            // 返回示例数据
            Map<String, Object> result = new HashMap<>();
            
            // 常见列名列表
            List<String> commonColumns = Arrays.asList(
                "username", "name", "phone", "mobile", "telephone", "email", 
                "address", "id_card", "idcard", "identity", "bank_card", 
                "bankcard", "card", "password", "birth_date", "gender", "age"
            );
            
            // 从服务获取所有脱敏规则
            List<DesensitizationRule> rules = desensitizationRuleService.getAllRules();
            
            // 构建规则映射选项
            Map<String, List<Map<String, Object>>> columnRuleMappings = new HashMap<>();
            
            // 为每个列名提供可能的规则选项
            for (String column : commonColumns) {
                List<Map<String, Object>> availableRules = new ArrayList<>();
                
                // 根据列名推荐合适的规则
                for (DesensitizationRule rule : rules) {
                    boolean isRecommended = isRuleRecommendedForColumn(column, rule.getType());
                    
                    Map<String, Object> ruleOption = new HashMap<>();
                    ruleOption.put("ruleId", rule.getRuleId());
                    ruleOption.put("name", rule.getName());
                    ruleOption.put("type", rule.getType());
                    ruleOption.put("pattern", rule.getPattern());
                    ruleOption.put("prefixLength", rule.getPrefixLength());
                    ruleOption.put("suffixLength", rule.getSuffixLength());
                    ruleOption.put("replacementChar", rule.getReplacementChar());
                    ruleOption.put("recommended", isRecommended);
                    
                    availableRules.add(ruleOption);
                }
                
                // 按推荐程度排序（推荐的规则排在前面）
                availableRules.sort((a, b) -> {
                    boolean aRecommended = (Boolean) a.get("recommended");
                    boolean bRecommended = (Boolean) b.get("recommended");
                    return bRecommended ? 1 : (aRecommended ? -1 : 0);
                });
                
                columnRuleMappings.put(column, availableRules);
            }
            
            result.put("commonColumns", commonColumns);
            result.put("allRules", rules);
            result.put("columnRuleMappings", columnRuleMappings);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取规则映射模板失败: " + e.getMessage());
        }
    }

    // 根据列名和规则类型判断是否推荐
    private boolean isRuleRecommendedForColumn(String columnName, String ruleType) {
        if (ruleType == null) {
            return false;
        }
        
        // 基于列名和规则类型的匹配逻辑
        switch (columnName.toLowerCase()) {
            case "username":
            case "name":
                return "NAME".equalsIgnoreCase(ruleType);
                
            case "phone":
            case "mobile":
            case "telephone":
                return "PHONE".equalsIgnoreCase(ruleType);
                
            case "email":
                return "EMAIL".equalsIgnoreCase(ruleType);
                
            case "address":
                return "ADDRESS".equalsIgnoreCase(ruleType);
                
            case "id_card":
            case "idcard":
            case "identity":
                return "ID_CARD".equalsIgnoreCase(ruleType);
                
            case "bank_card":
            case "bankcard":
            case "card":
                return "BANK_CARD".equalsIgnoreCase(ruleType);
                
            default:
                return false;
        }
    }

    // 将Task实体转换为Map（脱敏后返回）
    private Map<String, Object> convertTaskToMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("taskName", task.getTaskName());
        map.put("status", task.getStatus());
        map.put("createTime", task.getCreateTime().format(DATE_FORMATTER));
        map.put("sourceDatabase", task.getSourceDatabase());
        map.put("sourceTables", task.getSourceTables());
        
        // 添加新字段
        if (task.getPriority() != null) {
            map.put("priority", task.getPriority());
        }
        
        if (task.getOutputFormat() != null) {
            map.put("outputFormat", task.getOutputFormat());
        }
        
        if (task.getOutputLocation() != null) {
            map.put("outputLocation", task.getOutputLocation());
        }
        
        if (task.getOutputTable() != null) {
            map.put("outputTable", task.getOutputTable());
        }
        
        // 根据需要添加其他字段
        if (task.getExecuteTime() != null) {
            map.put("executeTime", task.getExecuteTime().format(DATE_FORMATTER));
        }
        
        if (task.getTaskDescription() != null) {
            map.put("taskDescription", task.getTaskDescription());
        }
        
        // 添加任务完成后的结果信息
        if ("已完成".equals(task.getStatus())) {
            // 添加执行日志摘要（最后100个字符）
            if (task.getExecutionLog() != null && !task.getExecutionLog().isEmpty()) {
                String log = task.getExecutionLog();
                if (log.length() > 100) {
                    map.put("executionSummary", "..." + log.substring(log.length() - 100));
                } else {
                    map.put("executionSummary", log);
                }
            }
            
            // 添加输出位置信息
            if ("CSV".equalsIgnoreCase(task.getOutputFormat()) || "JSON".equalsIgnoreCase(task.getOutputFormat())) {
                map.put("outputType", "文件");
                map.put("outputLocation", task.getOutputLocation());
            } else if ("DATABASE".equalsIgnoreCase(task.getOutputFormat())) {
                map.put("outputType", "数据库");
                map.put("outputTable", task.getOutputTable());
                // 判断是否为同库同表
                if (task.getSourceTables() != null && task.getSourceTables().equals(task.getOutputTable())) {
                    map.put("outputMode", "同库同表（覆盖原表）");
                } else {
                    map.put("outputMode", "新表");
                }
            }
        }
        
        return map;
    }
} 