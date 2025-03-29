package com.example.service.impl;

import com.example.model.DesensitizationRule;
import com.example.repository.DesensitizationRuleRepository;
import com.example.service.DesensitizationRuleService;
import com.example.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DesensitizationRuleServiceImpl implements DesensitizationRuleService {

    private static final Logger logger = LoggerFactory.getLogger(DesensitizationRuleServiceImpl.class);
    private static final String RULES_CACHE_KEY = "desensitization:rules:all";
    private static final String RULE_CACHE_KEY_PREFIX = "desensitization:rule:";
    private static final long CACHE_EXPIRE_HOURS = 1;

    @Autowired
    private DesensitizationRuleRepository ruleRepository;
    
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public List<DesensitizationRule> getAllRules() {
        try {
            // 尝试从缓存获取
            Object cachedRules = redisUtils.get(RULES_CACHE_KEY);
            if (cachedRules != null) {
                return (List<DesensitizationRule>) cachedRules;
            }
            
            // 如果缓存中没有，从数据库获取
            List<DesensitizationRule> rules = ruleRepository.findAll();
            
            // 存入缓存
            redisUtils.set(RULES_CACHE_KEY, rules, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            
            return rules;
        } catch (Exception e) {
            logger.error("获取脱敏规则列表失败", e);
            return ruleRepository.findAll();
        }
    }

    @Override
    public Optional<DesensitizationRule> getRuleById(String ruleId) {
        try {
            String cacheKey = RULE_CACHE_KEY_PREFIX + ruleId;
            
            // 尝试从缓存获取
            Object cachedRule = redisUtils.get(cacheKey);
            if (cachedRule != null) {
                return Optional.of((DesensitizationRule) cachedRule);
            }
            
            // 如果缓存中没有，从数据库获取
            Optional<DesensitizationRule> rule = ruleRepository.findByRuleId(ruleId);
            
            // 存入缓存
            rule.ifPresent(r -> redisUtils.set(cacheKey, r, CACHE_EXPIRE_HOURS, TimeUnit.HOURS));
            
            return rule;
        } catch (Exception e) {
            logger.error("获取脱敏规则失败: {}", ruleId, e);
            return ruleRepository.findByRuleId(ruleId);
        }
    }
    
    // 清除规则相关的所有缓存
    private void clearRuleCache(String ruleId) {
        // 清除规则列表缓存
        redisUtils.delete(RULES_CACHE_KEY);
        
        // 清除单个规则缓存
        redisUtils.delete(RULE_CACHE_KEY_PREFIX + ruleId);
    }
    
    // 清除所有规则缓存
    private void clearAllRuleCache() {
        // 清除规则列表缓存
        redisUtils.delete(RULES_CACHE_KEY);
        
        // 清除所有单个规则缓存
        redisUtils.deletePattern(RULE_CACHE_KEY_PREFIX + "*");
    }

    @Override
    @Transactional
    public DesensitizationRule createRule(Map<String, Object> ruleMap) {
        // 获取前端传入的 ruleId，如果没有则生成唯一 ID
        String ruleId;
        if (ruleMap.containsKey("ruleId")) {
            ruleId = (String) ruleMap.get("ruleId");
        } else if (ruleMap.containsKey("id") && ruleMap.get("id") instanceof String) {
            // 兼容处理：如果传入的是 id 而不是 ruleId，则使用 id 作为 ruleId
            ruleId = (String) ruleMap.get("id");
        } else {
            // 都没有提供则生成新的 UUID
            ruleId = UUID.randomUUID().toString().replace("-", "");
        }
        
        String name = (String) ruleMap.get("name");
        String description = (String) ruleMap.get("description");
        String type = (String) ruleMap.get("type");
        String pattern = (String) ruleMap.get("pattern");
        
        // 处理数字类型
        Integer prefixLength = null;
        if (ruleMap.containsKey("prefixLength")) {
            if (ruleMap.get("prefixLength") instanceof Integer) {
                prefixLength = (Integer) ruleMap.get("prefixLength");
            } else if (ruleMap.get("prefixLength") instanceof Number) {
                prefixLength = ((Number) ruleMap.get("prefixLength")).intValue();
            }
        }
        
        Integer suffixLength = null;
        if (ruleMap.containsKey("suffixLength")) {
            if (ruleMap.get("suffixLength") instanceof Integer) {
                suffixLength = (Integer) ruleMap.get("suffixLength");
            } else if (ruleMap.get("suffixLength") instanceof Number) {
                suffixLength = ((Number) ruleMap.get("suffixLength")).intValue();
            }
        }
        
        String replacementChar = (String) ruleMap.get("replacementChar");

        DesensitizationRule rule = new DesensitizationRule(
            ruleId, name, description, type, pattern, 
            prefixLength, suffixLength, replacementChar
        );
        
        DesensitizationRule savedRule = ruleRepository.save(rule);
        clearAllRuleCache();
        return savedRule;
    }

    @Override
    @Transactional
    public Optional<DesensitizationRule> updateRule(String ruleId, Map<String, Object> ruleMap) {
        Optional<DesensitizationRule> existingRuleOpt = ruleRepository.findByRuleId(ruleId);
        
        if (existingRuleOpt.isPresent()) {
            DesensitizationRule existingRule = existingRuleOpt.get();
            
            if (ruleMap.containsKey("name")) {
                existingRule.setName((String) ruleMap.get("name"));
            }
            
            if (ruleMap.containsKey("description")) {
                existingRule.setDescription((String) ruleMap.get("description"));
            }
            
            if (ruleMap.containsKey("type")) {
                existingRule.setType((String) ruleMap.get("type"));
            }
            
            if (ruleMap.containsKey("pattern")) {
                existingRule.setPattern((String) ruleMap.get("pattern"));
            }
            
            if (ruleMap.containsKey("prefixLength")) {
                if (ruleMap.get("prefixLength") instanceof Integer) {
                    existingRule.setPrefixLength((Integer) ruleMap.get("prefixLength"));
                } else if (ruleMap.get("prefixLength") instanceof Number) {
                    existingRule.setPrefixLength(((Number) ruleMap.get("prefixLength")).intValue());
                }
            }
            
            if (ruleMap.containsKey("suffixLength")) {
                if (ruleMap.get("suffixLength") instanceof Integer) {
                    existingRule.setSuffixLength((Integer) ruleMap.get("suffixLength"));
                } else if (ruleMap.get("suffixLength") instanceof Number) {
                    existingRule.setSuffixLength(((Number) ruleMap.get("suffixLength")).intValue());
                }
            }
            
            if (ruleMap.containsKey("replacementChar")) {
                existingRule.setReplacementChar((String) ruleMap.get("replacementChar"));
            }
            
            DesensitizationRule updatedRule = ruleRepository.save(existingRule);
            clearRuleCache(ruleId);
            return Optional.of(updatedRule);
        }
        
        return Optional.empty();
    }

    @Override
    @Transactional
    public boolean deleteRule(String ruleId) {
        if (ruleRepository.existsByRuleId(ruleId)) {
            ruleRepository.deleteByRuleId(ruleId);
            clearRuleCache(ruleId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void initDefaultRules() {
        // 清除所有现有规则
        ruleRepository.deleteAll();
        clearAllRuleCache();
        
        // 创建默认规则
        createRule(Map.of(
            "ruleId", "default_phone",
            "name", "手机号码脱敏",
            "description", "默认的手机号码脱敏规则",
            "type", "PHONE",
            "pattern", "^(\\d{3})\\d{4}(\\d{4})$",
            "replacementChar", "*"
        ));
        
        createRule(Map.of(
            "ruleId", "default_email",
            "name", "邮箱地址脱敏",
            "description", "默认的邮箱地址脱敏规则",
            "type", "EMAIL",
            "pattern", "^(\\w{3})[^@]*@(\\w+)$",
            "replacementChar", "*"
        ));
        
        createRule(Map.of(
            "ruleId", "default_idcard",
            "name", "身份证号脱敏",
            "description", "默认的身份证号脱敏规则",
            "type", "ID_CARD",
            "pattern", "^(\\d{6})\\d{8}(\\d{4})$",
            "replacementChar", "*"
        ));
        
        createRule(Map.of(
            "ruleId", "default_name",
            "name", "姓名脱敏",
            "description", "默认的姓名脱敏规则",
            "type", "NAME",
            "pattern", "KEEP_PREFIX",
            "prefixLength", 1,
            "replacementChar", "*"
        ));
        
        createRule(Map.of(
            "ruleId", "default_bankcard",
            "name", "银行卡号脱敏",
            "description", "默认的银行卡号脱敏规则",
            "type", "BANK_CARD",
            "pattern", "KEEP_SUFFIX",
            "suffixLength", 4,
            "replacementChar", "*"
        ));
        
        createRule(Map.of(
            "ruleId", "default_address",
            "name", "地址脱敏",
            "description", "默认的地址脱敏规则",
            "type", "ADDRESS",
            "pattern", "KEEP_PREFIX",
            "prefixLength", 10,
            "replacementChar", "*"
        ));
    }
} 