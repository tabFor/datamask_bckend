package com.example.service;

import com.example.dto.MaskingRuleDTO;
import com.example.dto.MaskingRuleRequest;
import com.example.dto.MaskingRuleResponse;
import com.example.model.MaskingRuleEntity;
import com.example.repository.MaskingRuleRepository;
import com.example.interceptor.SQLMaskingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaskingRuleService {

    private static final Logger logger = LoggerFactory.getLogger(MaskingRuleService.class);
    private final MaskingRuleRepository maskingRuleRepository;
    private final SQLMaskingInterceptor sqlMaskingInterceptor;

    @Autowired
    public MaskingRuleService(
            MaskingRuleRepository maskingRuleRepository,
            SQLMaskingInterceptor sqlMaskingInterceptor) {
        this.maskingRuleRepository = maskingRuleRepository;
        this.sqlMaskingInterceptor = sqlMaskingInterceptor;
    }
    
    /**
     * 系统启动时自动加载所有活跃的脱敏规则
     */
    @PostConstruct
    public void loadAllRulesOnStartup() {
        logger.info("系统启动，开始加载所有脱敏规则...");
        try {
            List<String> tables = getAllTables();
            logger.info("找到 {} 个表配置了脱敏规则", tables.size());
            
            for (String table : tables) {
                refreshRules(table);
                logger.info("已加载表 {} 的脱敏规则", table);
            }
            
            // 打印当前所有规则，以便确认是否加载成功
            sqlMaskingInterceptor.printAllMaskingRules();
            logger.info("所有脱敏规则加载完成");
        } catch (Exception e) {
            logger.error("加载脱敏规则时出错: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public List<MaskingRuleEntity> getAllRules() {
        return maskingRuleRepository.findAll();
    }

    @Transactional
    public List<MaskingRuleEntity> getActiveRules() {
        return maskingRuleRepository.findByActiveIsTrue();
    }

    @Transactional
    public List<MaskingRuleEntity> getRulesByTable(String tableName) {
        return maskingRuleRepository.findByTableName(tableName);
    }

    @Transactional
    public List<MaskingRuleEntity> getActiveRulesByTable(String tableName) {
        return maskingRuleRepository.findByTableNameAndActiveTrue(tableName);
    }

    @Transactional
    public MaskingRuleEntity saveRule(MaskingRuleEntity rule) {
        MaskingRuleEntity savedRule = maskingRuleRepository.save(rule);
        updateInterceptorRules(rule.getTableName());
        return savedRule;
    }

    @Transactional
    public void deleteRule(Long id) {
        MaskingRuleEntity rule = maskingRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        maskingRuleRepository.deleteById(id);
        updateInterceptorRules(rule.getTableName());
    }

    @Transactional
    public void updateRuleStatus(Long id, boolean active) {
        MaskingRuleEntity rule = maskingRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        rule.setActive(active);
        maskingRuleRepository.save(rule);
        updateInterceptorRules(rule.getTableName());
    }

    private void updateInterceptorRules(String tableName) {
        List<MaskingRuleEntity> rules = maskingRuleRepository.findByTableNameAndActiveTrue(tableName);
        sqlMaskingInterceptor.updateMaskingRules(tableName, rules);
    }

    @Transactional
    public MaskingRuleResponse updateRules(MaskingRuleRequest request) {
        try {
            // 清空现有规则（可选，取决于业务需求）
            // maskingRuleRepository.deleteAll();
            
            // 保存新规则
            List<MaskingRuleEntity> savedEntities = request.getRules().stream()
                    .map(dto -> {
                        MaskingRuleEntity rule = new MaskingRuleEntity();
                        rule.setDatabase(dto.getDatabase());
                        rule.setTableName(dto.getTableName());
                        rule.setColumnName(dto.getColumnName());
                        rule.setMaskingType(dto.getMaskingType());
                        rule.setActive(dto.isActive());
                        return maskingRuleRepository.save(rule);
                    })
                    .collect(Collectors.toList());
            
            List<MaskingRuleDTO> savedDTOs = savedEntities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            return new MaskingRuleResponse(savedDTOs, "脱敏规则更新成功", true);
        } catch (Exception e) {
            return new MaskingRuleResponse(null, "脱敏规则更新失败: " + e.getMessage(), false);
        }
    }

    private MaskingRuleDTO convertToDTO(MaskingRuleEntity entity) {
        return new MaskingRuleDTO(
                entity.getId(),
                entity.getDatabase(),
                entity.getTableName(),
                entity.getColumnName(),
                entity.getMaskingType(),
                entity.isActive()
        );
    }

    private MaskingRuleEntity convertToEntity(MaskingRuleDTO dto) {
        MaskingRuleEntity entity = new MaskingRuleEntity();
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }
        entity.setDatabase(dto.getDatabase());
        entity.setTableName(dto.getTableName());
        entity.setColumnName(dto.getColumnName());
        entity.setMaskingType(dto.getMaskingType());
        entity.setActive(dto.isActive());
        return entity;
    }

    @Transactional
    public List<String> getAllTables() {
        return maskingRuleRepository.findAll().stream()
                .map(MaskingRuleEntity::getTableName)
                .distinct()
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void refreshRules(String tableName) {
        updateInterceptorRules(tableName);
    }

    /**
     * 清空所有脱敏规则
     */
    @Transactional
    public void clearMaskingRules() {
        sqlMaskingInterceptor.clearMaskingRules();
    }
} 