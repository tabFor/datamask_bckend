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
import com.example.model.MaskingFact;
import org.kie.api.runtime.KieSession;

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

    @Autowired
    private KieSession kieSession;

    @PostConstruct
    public void init() {
        System.out.println("MaskingRule bean successfully injected: " + maskingRule);
    }

    @Override
    public String maskValue(String value, String type, String pattern, Integer prefixLength, Integer suffixLength, String replacementChar) {
        if (value == null) return null;
        
        // 创建规则事实
        MaskingFact fact = new MaskingFact(value, type, pattern, prefixLength, suffixLength, replacementChar);
        
        // 执行规则
        kieSession.insert(fact);
        kieSession.fireAllRules();
        
        return fact.getMaskedValue();
    }

    @Override
    public List<Map<String, Object>> maskData(List<Map<String, Object>> data, List<Map<String, Object>> rules) {
        List<Map<String, Object>> maskedData = new ArrayList<>();
        
        for (Map<String, Object> item : data) {
            Map<String, Object> maskedItem = new HashMap<>(item);
            
            for (Map<String, Object> rule : rules) {
                String columnName = extractColumnName(rule);
                String maskingType = extractMaskingType(rule);
                
                if (maskedItem.containsKey(columnName)) {
                    Object value = maskedItem.get(columnName);
                    if (value != null) {
                        maskedItem.put(columnName, maskValue(
                            value.toString(),
                            maskingType,
                            null,
                            1,
                            1,
                            "*"
                        ));
                    }
                }
            }
            
            maskedData.add(maskedItem);
        }
        
        return maskedData;
    }

    private String extractColumnName(Map<String, Object> rule) {
        return (String) rule.get("columnName");
    }

    private String extractMaskingType(Map<String, Object> rule) {
        return (String) rule.get("maskingType");
    }
}