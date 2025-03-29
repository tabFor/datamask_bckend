package com.example.service;

import java.util.List;
import java.util.Map;

public interface DataMaskingService {
    String maskUsername(String username);
    String maskEmail(String email);
    String maskPhone(String phone);
    String maskIdCard(String idCard);
    String maskBankCard(String bankCard);
    String maskAddress(String address);
    
    // 获取脱敏后的数据
    List<Map<String, Object>> getMaskedData(Map<String, Object> maskingParams);
    
    // 创建目标表并插入脱敏后的数据
    boolean createTableAndInsertData(String targetTable, List<Map<String, Object>> maskedData);
}