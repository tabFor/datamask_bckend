package com.example.repository;

import com.example.model.DesensitizationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesensitizationRuleRepository extends JpaRepository<DesensitizationRule, Long> {
    
    /**
     * 根据规则ID查找规则
     */
    Optional<DesensitizationRule> findByRuleId(String ruleId);
    
    /**
     * 根据类型查找规则
     */
    Optional<DesensitizationRule> findByType(String type);
    
    /**
     * 检查规则ID是否存在
     */
    boolean existsByRuleId(String ruleId);
    
    /**
     * 根据规则ID删除规则
     */
    void deleteByRuleId(String ruleId);
} 