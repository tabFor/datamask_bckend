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
import com.example.service.StaticDataMaskingService;

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
    private StaticDataMaskingService staticDataMaskingService;

    @PostConstruct
    public void init() {
        System.out.println("MaskingRule bean successfully injected: " + maskingRule);
    }

    @Override
    public String maskValue(String value, String type, String pattern, Integer prefixLength, Integer suffixLength, String replacementChar) {
        if (value == null) return null;
        
        logger.info("开始执行脱敏规则，原始值: {}, 类型: {}", value, type);
        
        // 使用StaticDataMaskingService进行脱敏
        Map<String, Object> rule = new HashMap<>();
        rule.put("maskingType", type);
        rule.put("prefixLength", prefixLength);
        rule.put("suffixLength", suffixLength);
        rule.put("replacementChar", replacementChar);
        
        String maskedValue = (String) staticDataMaskingService.applyMasking(value, type, rule);
        
        logger.info("脱敏完成，脱敏后值: {}", maskedValue);
        
        return maskedValue;
    }

    @Override
    public List<Map<String, Object>> maskData(List<Map<String, Object>> data, List<Map<String, Object>> rules) {
        return staticDataMaskingService.processMaskedData(data, rules);
    }
}