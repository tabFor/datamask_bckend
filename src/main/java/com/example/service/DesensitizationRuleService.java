package com.example.service;

import com.example.model.DesensitizationRule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DesensitizationRuleService {
    
    /**
     * 获取所有脱敏规则
     */
    List<DesensitizationRule> getAllRules();
    
    /**
     * 根据ID获取脱敏规则
     */
    Optional<DesensitizationRule> getRuleById(String ruleId);
    
    /**
     * 创建脱敏规则
     */
    DesensitizationRule createRule(Map<String, Object> ruleMap);
    
    /**
     * 更新脱敏规则
     */
    Optional<DesensitizationRule> updateRule(String ruleId, Map<String, Object> ruleMap);
    
    /**
     * 删除脱敏规则
     */
    boolean deleteRule(String ruleId);
    
    /**
     * 初始化默认脱敏规则
     */
    void initDefaultRules();
} 