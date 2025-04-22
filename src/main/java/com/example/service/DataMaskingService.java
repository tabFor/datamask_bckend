package com.example.service;

import java.util.List;
import java.util.Map;

public interface DataMaskingService {
    
    /**
     * 对单个值进行脱敏处理
     */
    String maskValue(String value, String type, String pattern, Integer prefixLength, Integer suffixLength, String replacementChar);
    
    /**
     * 对数据集进行脱敏处理
     */
    List<Map<String, Object>> maskData(List<Map<String, Object>> data, List<Map<String, Object>> rules);
}