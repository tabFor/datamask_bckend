package com.example.aspect;

import com.example.service.AuditLogService;
import com.example.util.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    // 操作类型映射
    private static final Map<String, String> OPERATION_TYPE_MAP = new HashMap<>();
    static {
        // 系统相关
        OPERATION_TYPE_MAP.put("hello", "system_welcome");
        
        // 用户认证相关
        OPERATION_TYPE_MAP.put("login", "user_login");
        OPERATION_TYPE_MAP.put("checkLogin", "check_login_status");
        
        // 数据库管理相关
        OPERATION_TYPE_MAP.put("testConnection", "test_db_connection");
        OPERATION_TYPE_MAP.put("getTables", "get_db_tables");
        OPERATION_TYPE_MAP.put("getColumns", "get_table_columns");
        
        // 脱敏规则相关
        OPERATION_TYPE_MAP.put("getAllRules", "get_all_rules");
        OPERATION_TYPE_MAP.put("getActiveRules", "get_active_rules");
        OPERATION_TYPE_MAP.put("saveRule", "save_rule");
        OPERATION_TYPE_MAP.put("deleteRule", "delete_rule");
        OPERATION_TYPE_MAP.put("updateRule", "update_rule");
        OPERATION_TYPE_MAP.put("getRulesMappingTemplate", "get_rules_template");
        
        // 任务管理相关
        OPERATION_TYPE_MAP.put("createTask", "create_task");
        OPERATION_TYPE_MAP.put("executeTask", "execute_task");
        OPERATION_TYPE_MAP.put("getTaskStatus", "get_task_status");
        OPERATION_TYPE_MAP.put("deleteTask", "delete_task");
        OPERATION_TYPE_MAP.put("getTasks", "get_tasks");
        OPERATION_TYPE_MAP.put("getTask", "get_task");
        
        // 数据查看相关
        OPERATION_TYPE_MAP.put("getMaskedUsers", "get_masked_users");
        OPERATION_TYPE_MAP.put("getMaskedOrders", "get_masked_orders");
        OPERATION_TYPE_MAP.put("previewMaskedData", "preview_masked_data");
        OPERATION_TYPE_MAP.put("downloadMaskedData", "download_masked_data");
        OPERATION_TYPE_MAP.put("getCustomers", "get_customers");
        OPERATION_TYPE_MAP.put("getDataStats", "get_data_stats");
        OPERATION_TYPE_MAP.put("getOrders", "get_orders");
        OPERATION_TYPE_MAP.put("getOnlineTransactions", "get_online_transactions");
        OPERATION_TYPE_MAP.put("getCustomerById", "get_customer_by_id");
        OPERATION_TYPE_MAP.put("searchCustomersByName", "search_customers_by_name");
        OPERATION_TYPE_MAP.put("searchCustomersByAgeBetween", "search_customers_by_age");
        OPERATION_TYPE_MAP.put("getOnlineTransactionsByPaymentMethod", "get_transactions_by_payment");
        OPERATION_TYPE_MAP.put("searchCustomersByGender", "search_customers_by_gender");
        OPERATION_TYPE_MAP.put("getFinancialRecords", "get_financial_records");
        OPERATION_TYPE_MAP.put("getMedicalRecords", "get_medical_records");
        OPERATION_TYPE_MAP.put("getEmployeeData", "get_employee_data");
        OPERATION_TYPE_MAP.put("getFinancialRecordsByDateRange", "get_financial_records_by_date");
        OPERATION_TYPE_MAP.put("getMedicalRecordsByPatientId", "get_medical_records_by_patient");
        OPERATION_TYPE_MAP.put("getEmployeeDataByDepartment", "get_employee_data_by_dept");
        
        // 敏感数据检测相关
        OPERATION_TYPE_MAP.put("detectTableSensitiveColumns", "detect_table_sensitive_columns");
        OPERATION_TYPE_MAP.put("detectAllSensitiveColumns", "detect_all_sensitive_columns");
        OPERATION_TYPE_MAP.put("detectColumn", "detect_column");
        
        // 审计日志相关
        OPERATION_TYPE_MAP.put("searchLogs", "search_audit_logs");
        
        // 动态脱敏相关
        OPERATION_TYPE_MAP.put("query", "dynamic_query");
        
        // 脱敏数据管理相关
        OPERATION_TYPE_MAP.put("listMaskedDataFiles", "list_masked_files");
        OPERATION_TYPE_MAP.put("queryMaskedDatabaseTable", "query_masked_table");
        OPERATION_TYPE_MAP.put("updateMaskedData", "update_masked_data");
        OPERATION_TYPE_MAP.put("deleteMaskedData", "delete_masked_data");
        
        // 默认映射
        OPERATION_TYPE_MAP.put("default", "system_operation");
    }

    @Around("execution(* com.example.controller..*.*(..))")
    public Object logControllerOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader("Authorization");
        String username = "unknown_user";
        String operation = "";
        String details = "";
        String status = "success";

        // 执行原方法，我们先执行操作，再决定是否记录日志
        Object result = joinPoint.proceed();

        try {
            // 从token中获取用户名
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                username = JwtUtil.extractUsername(token);
            }

            // 获取操作信息
            String method = request.getMethod();
            String path = request.getRequestURI();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            
            // 获取操作类型
            String operationType = OPERATION_TYPE_MAP.getOrDefault(methodName, 
                OPERATION_TYPE_MAP.getOrDefault(className + "." + methodName, 
                OPERATION_TYPE_MAP.get("default")));
            
            // 构建操作描述
            operation = operationType;
            details = String.format("Method: %s, Path: %s", method, path);

            // 检查操作是否需要记录
            if (com.example.controller.AuditLogController.shouldLogOperation(operation)) {
                // 记录审计日志
                auditLogService.createLog(username, operation, details, status);
            }

            return result;

        } catch (Exception e) {
            status = "failed";
            details += ", Error: " + e.getMessage();
            
            // 错误情况下也需要检查是否记录日志
            if (com.example.controller.AuditLogController.shouldLogOperation(operation)) {
                auditLogService.createLog(username, operation, details, status);
            }
            
            throw e;
        }
    }
} 