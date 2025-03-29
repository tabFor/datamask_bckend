package com.example.service;

import com.example.model.AuditLog;
import com.example.repository.AuditLogRepository;
import com.example.utils.RedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private static final String AUDIT_LOG_LIST_CACHE_KEY_PREFIX = "audit:log:list:";
    private static final long CACHE_EXPIRE_HOURS = 1;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ObjectMapper objectMapper;

    public AuditLog createLog(String username, String operation, String details, String status) {
        // 检查操作是否需要记录
        if (!com.example.controller.AuditLogController.shouldLogOperation(operation)) {
            // 如果是被忽略的操作，返回null
            return null;
        }
        
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now(ZoneId.systemDefault()));
        log.setUsername(username);
        log.setOperation(operation);
        log.setDetails(details);
        log.setStatus(status);
        AuditLog savedLog = auditLogRepository.save(log);
        
        // 清除审计日志列表缓存
        clearAuditLogListCache();
        
        return savedLog;
    }

    public Page<AuditLog> searchLogs(LocalDateTime startTime, LocalDateTime endTime, 
                                   List<String> operations, int page, int size) {
        // 确保页码不小于1
        page = Math.max(1, page);
        
        String cacheKey = AUDIT_LOG_LIST_CACHE_KEY_PREFIX + 
                         (startTime != null ? startTime.toString() : "null") + ":" +
                         (endTime != null ? endTime.toString() : "null") + ":" +
                         (operations != null ? String.join(",", operations) : "null") + ":" +
                         page + ":" + size;
        
        // 尝试从缓存获取
        try {
            Object cachedData = redisUtils.get(cacheKey);
            if (cachedData != null) {
                Map<String, Object> map;
                if (cachedData instanceof String) {
                    map = objectMapper.readValue((String) cachedData, Map.class);
                } else if (cachedData instanceof Map) {
                    map = (Map<String, Object>) cachedData;
                } else {
                    // 如果类型不匹配，清除缓存并从数据库查询
                    redisUtils.delete(cacheKey);
                    throw new Exception("缓存数据类型不匹配");
                }
                
                List<AuditLog> content = objectMapper.convertValue(map.get("content"), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AuditLog.class));
                long totalElements = ((Number) map.get("totalElements")).longValue();
                int totalPages = ((Number) map.get("totalPages")).intValue();
                int currentPage = ((Number) map.get("number")).intValue();
                int currentSize = ((Number) map.get("size")).intValue();
                
                return new PageImpl<>(content, PageRequest.of(currentPage, currentSize), totalElements);
            }
        } catch (Exception e) {
            // 如果缓存读取失败，继续从数据库查询
            redisUtils.delete(cacheKey);
        }

        // 如果缓存中没有或缓存数据无效，从数据库获取
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
            }
            if (operations != null && !operations.isEmpty()) {
                if (operations.contains("system_welcome")) {
                    // 如果请求system_welcome，返回所有未定义的操作类型
                    predicates.add(cb.not(root.get("operation").in(Arrays.asList(
                        "user_login", "check_login_status", "test_db_connection", "get_db_tables",
                        "get_table_columns", "get_all_rules", "get_active_rules", "save_rule",
                        "delete_rule", "update_rule", "get_rules_template", "create_task",
                        "execute_task", "get_task_status", "delete_task", "get_tasks",
                        "get_task", "get_masked_users", "get_masked_orders", "preview_masked_data",
                        "download_masked_data", "get_customers", "get_data_stats", "get_orders",
                        "get_online_transactions", "get_customer_by_id", "search_customers_by_name",
                        "search_customers_by_age", "get_transactions_by_payment", "search_customers_by_gender",
                        "get_financial_records", "get_medical_records", "get_employee_data",
                        "get_financial_records_by_date", "get_medical_records_by_patient",
                        "get_employee_data_by_dept", "detect_table_sensitive_columns",
                        "detect_all_sensitive_columns", "detect_column", "search_audit_logs",
                        "dynamic_query", "list_masked_files", "query_masked_table",
                        "update_masked_data", "delete_masked_data", "system_operation"
                    ))));
                } else {
                    predicates.add(root.get("operation").in(operations));
                }
            }
            
            // 添加默认排序
            query.orderBy(cb.desc(root.get("timestamp")));
            
            // 如果没有其他条件，返回true以获取所有记录
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageRequest);
        
        // 存入缓存
        try {
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("content", result.getContent());
            cacheData.put("totalElements", result.getTotalElements());
            cacheData.put("totalPages", result.getTotalPages());
            cacheData.put("number", result.getNumber());
            cacheData.put("size", result.getSize());
            String jsonData = objectMapper.writeValueAsString(cacheData);
            redisUtils.set(cacheKey, jsonData, CACHE_EXPIRE_HOURS, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            // 如果缓存失败，记录错误但继续执行
        }
        
        return result;
    }

    // 清除审计日志列表缓存
    private void clearAuditLogListCache() {
        redisUtils.deletePattern(AUDIT_LOG_LIST_CACHE_KEY_PREFIX + "*");
    }
} 