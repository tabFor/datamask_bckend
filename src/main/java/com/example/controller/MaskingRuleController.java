package com.example.controller;

import com.example.model.MaskingRuleEntity;
import com.example.service.MaskingRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masking-rules")
public class MaskingRuleController {
    
    private static final Logger logger = LoggerFactory.getLogger(MaskingRuleController.class);
    
    private final MaskingRuleService maskingRuleService;
    
    @Autowired
    public MaskingRuleController(MaskingRuleService maskingRuleService) {
        this.maskingRuleService = maskingRuleService;
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRules() {
        List<MaskingRuleEntity> rules = maskingRuleService.getAllRules();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("rules", rules);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveRules() {
        List<MaskingRuleEntity> rules = maskingRuleService.getActiveRules();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("rules", rules);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveRule(@RequestBody Object rawRequest) {
        Map<String, Object> response = new HashMap<>();
        
        MaskingRuleEntity rule;
        
        // 处理不同类型的请求
        if (rawRequest instanceof Map) {
            Map<String, Object> requestMap = (Map<String, Object>) rawRequest;
            
            // 检查是否包含rules字段（处理包装的情况）
            if (requestMap.containsKey("rules")) {
                Object rulesObj = requestMap.get("rules");
                
                if (rulesObj instanceof List && !((List<?>) rulesObj).isEmpty()) {
                    List<?> rulesList = (List<?>) rulesObj;
                    
                    // 处理第一个规则（这里只处理单个规则，多个规则请使用/batch端点）
                    Object firstRule = rulesList.get(0);
                    if (firstRule instanceof Map) {
                        rule = convertMapToEntity((Map<String, Object>) firstRule);
                    } else {
                        logger.warn("规则数据格式不正确");
                        response.put("success", false);
                        response.put("message", "无法解析规则数据，请使用标准格式");
                        return ResponseEntity.badRequest().body(response);
                    }
                } else {
                    logger.warn("rules字段为空或不是列表");
                    response.put("success", false);
                    response.put("message", "rules字段必须是一个非空数组");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                // 直接处理Map作为规则
                rule = convertMapToEntity(requestMap);
            }
        } else if (rawRequest instanceof MaskingRuleEntity) {
            // 如果已经是实体对象，直接使用
            rule = (MaskingRuleEntity) rawRequest;
        } else {
            logger.warn("不支持的请求数据类型: {}", rawRequest.getClass().getName());
            response.put("success", false);
            response.put("message", "不支持的请求数据格式");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 验证规则数据
        if (rule.getColumnName() == null || rule.getColumnName().isEmpty()) {
            logger.warn("列名字段为空");
            response.put("success", false);
            response.put("message", "列名(columnName)字段不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (rule.getDatabase() == null || rule.getDatabase().isEmpty()) {
            logger.warn("数据库名字段为空");
            response.put("success", false);
            response.put("message", "数据库名(database)字段不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (rule.getTableName() == null || rule.getTableName().isEmpty()) {
            logger.warn("表名字段为空");
            response.put("success", false);
            response.put("message", "表名(tableName)字段不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (rule.getMaskingType() == null || rule.getMaskingType().isEmpty()) {
            logger.warn("脱敏类型字段为空");
            response.put("success", false);
            response.put("message", "脱敏类型(maskingType)字段不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            MaskingRuleEntity savedRule = maskingRuleService.saveRule(rule);
            
            response.put("success", true);
            response.put("message", "规则保存成功");
            response.put("rule", savedRule);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("保存规则时发生错误: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "保存规则时发生错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // 辅助方法：将Map转换为实体对象
    private MaskingRuleEntity convertMapToEntity(Map<String, Object> map) {
        MaskingRuleEntity entity = new MaskingRuleEntity();
        
        if (map.containsKey("id") && map.get("id") != null) {
            if (map.get("id") instanceof Number) {
                entity.setId(((Number) map.get("id")).longValue());
            } else if (map.get("id") instanceof String) {
                try {
                    entity.setId(Long.parseLong((String) map.get("id")));
                } catch (NumberFormatException e) {
                    logger.debug("无法解析ID值: {}", map.get("id"));
                }
            }
        }
        
        if (map.containsKey("database")) {
            entity.setDatabase((String) map.get("database"));
        }
        
        if (map.containsKey("tableName")) {
            entity.setTableName((String) map.get("tableName"));
        }
        
        if (map.containsKey("columnName")) {
            entity.setColumnName((String) map.get("columnName"));
        }
        
        if (map.containsKey("maskingType")) {
            entity.setMaskingType((String) map.get("maskingType"));
        }
        
        if (map.containsKey("active")) {
            if (map.get("active") instanceof Boolean) {
                entity.setActive((Boolean) map.get("active"));
            } else if (map.get("active") instanceof String) {
                entity.setActive(Boolean.parseBoolean((String) map.get("active")));
            }
        }
        
        return entity;
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        maskingRuleService.deleteRule(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "规则删除成功");
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateRuleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        maskingRuleService.updateRuleStatus(id, active);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "规则状态更新成功");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> saveBatchRules(@RequestBody List<MaskingRuleEntity> rules) {
        Map<String, Object> response = new HashMap<>();
        
        if (rules == null || rules.isEmpty()) {
            response.put("success", false);
            response.put("message", "请求中没有找到规则数据");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 检查规则数据
        for (int i = 0; i < rules.size(); i++) {
            MaskingRuleEntity rule = rules.get(i);
            
            if (rule.getColumnName() == null || rule.getColumnName().isEmpty()) {
                response.put("success", false);
                response.put("message", "第" + (i + 1) + "条规则的列名(columnName)字段不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (rule.getDatabase() == null || rule.getDatabase().isEmpty()) {
                response.put("success", false);
                response.put("message", "第" + (i + 1) + "条规则的数据库名(database)字段不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (rule.getTableName() == null || rule.getTableName().isEmpty()) {
                response.put("success", false);
                response.put("message", "第" + (i + 1) + "条规则的表名(tableName)字段不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (rule.getMaskingType() == null || rule.getMaskingType().isEmpty()) {
                response.put("success", false);
                response.put("message", "第" + (i + 1) + "条规则的脱敏类型(maskingType)字段不能为空");
                return ResponseEntity.badRequest().body(response);
            }
        }
        
        // 保存所有规则
        List<MaskingRuleEntity> savedRules = new java.util.ArrayList<>();
        try {
            for (MaskingRuleEntity rule : rules) {
                MaskingRuleEntity savedRule = maskingRuleService.saveRule(rule);
                savedRules.add(savedRule);
            }
            
            response.put("success", true);
            response.put("message", "批量规则保存成功");
            response.put("rules", savedRules);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("保存规则时发生错误: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "保存规则时发生错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/batch-wrapped")
    public ResponseEntity<Map<String, Object>> saveBatchWrappedRules(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取rules数组
            List<Map<String, Object>> rulesData = (List<Map<String, Object>>) request.get("rules");
            if (rulesData == null || rulesData.isEmpty()) {
                response.put("success", false);
                response.put("message", "请求中没有找到规则数据");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 转换为实体对象
            List<MaskingRuleEntity> rules = new java.util.ArrayList<>();
            for (Map<String, Object> ruleData : rulesData) {
                MaskingRuleEntity rule = new MaskingRuleEntity();
                
                // 设置ID（如果有）
                if (ruleData.containsKey("id")) {
                    rule.setId(((Number) ruleData.get("id")).longValue());
                }
                
                // 设置其他字段
                if (ruleData.containsKey("database")) {
                    rule.setDatabase((String) ruleData.get("database"));
                }
                
                if (ruleData.containsKey("tableName")) {
                    rule.setTableName((String) ruleData.get("tableName"));
                }
                
                if (ruleData.containsKey("columnName")) {
                    rule.setColumnName((String) ruleData.get("columnName"));
                }
                
                if (ruleData.containsKey("maskingType")) {
                    rule.setMaskingType((String) ruleData.get("maskingType"));
                }
                
                if (ruleData.containsKey("active")) {
                    rule.setActive((Boolean) ruleData.get("active"));
                }
                
                rules.add(rule);
            }
            
            // 验证规则
            for (int i = 0; i < rules.size(); i++) {
                MaskingRuleEntity rule = rules.get(i);
                
                if (rule.getColumnName() == null || rule.getColumnName().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "第" + (i + 1) + "条规则的列名(columnName)字段不能为空");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (rule.getDatabase() == null || rule.getDatabase().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "第" + (i + 1) + "条规则的数据库名(database)字段不能为空");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (rule.getTableName() == null || rule.getTableName().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "第" + (i + 1) + "条规则的表名(tableName)字段不能为空");
                    return ResponseEntity.badRequest().body(response);
                }
                
                if (rule.getMaskingType() == null || rule.getMaskingType().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "第" + (i + 1) + "条规则的脱敏类型(maskingType)字段不能为空");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 保存所有规则
            List<MaskingRuleEntity> savedRules = new java.util.ArrayList<>();
            for (MaskingRuleEntity rule : rules) {
                MaskingRuleEntity savedRule = maskingRuleService.saveRule(rule);
                savedRules.add(savedRule);
            }
            
            response.put("success", true);
            response.put("message", "批量规则保存成功");
            response.put("rules", savedRules);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("处理包装规则时发生错误: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "处理规则时发生错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshRules() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取所有表的规则
            List<String> tables = maskingRuleService.getAllTables();
            for (String table : tables) {
                maskingRuleService.refreshRules(table);
            }
            
            response.put("success", true);
            response.put("message", "脱敏规则缓存已刷新");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("刷新规则缓存时发生错误: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "刷新规则缓存时发生错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}