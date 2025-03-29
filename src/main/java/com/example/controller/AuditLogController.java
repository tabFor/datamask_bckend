package com.example.controller;

import com.example.model.AuditLog;
import com.example.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    // 定义不需要记录的操作列表
    private static final List<String> IGNORED_OPERATIONS = Arrays.asList(
            // 日志查看相关
            "view_logs", 
            "search_logs", 
            "export_logs", 
            "audit_log_view", 
            "audit_log_search",
            "search_audit_logs",
            "GET /api/audit-logs/search",
            
            // 日志服务相关操作
            "log_service",
            "log_visualization",
            "log_export",
            "log_statistics",
            "log_cleanup",
            
            // 系统内部日志相关
            "system_log",
            "debug_log",
            "trace_log",
            "health_check",
            "metrics_collection",
            
            // 静态资源请求
            "GET /css/**",
            "GET /js/**",
            "GET /images/**",
            "GET /assets/**",
            "GET /fonts/**",
            "GET /favicon.ico",
            
            // Swagger API文档相关
            "GET /swagger-ui.html",
            "GET /swagger-ui/**",
            "GET /v3/api-docs/**",
            "GET /swagger-resources/**",
            "GET /webjars/**",
            
            // 心跳检测和健康检查
            "GET /actuator/**",
            "GET /actuator/health",
            "GET /actuator/info",
            "health_check",
            "ping",
            "system_status",
            
            // 用户状态检查
            "GET /api/check-login",
            "check_login_status",
            
            // 预览和元数据
            "preview_masked_data",
            "get_data_stats",
            "GET /api/masked-data/preview",
            
            // 脱敏数据库表相关操作
            "GET /api/masked-data/db-tables",
            "GET /api/masked-data/db-query",
            "list_masked_tables",
            "query_masked_table",
            "get_data_columns"
    );

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/search")
    public ResponseEntity<Page<AuditLog>> searchLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) List<String> operations,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<AuditLog> logs = auditLogService.searchLogs(startTime, endTime, operations, page, size);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 检查操作是否应该被记录
     * @param operation 操作名称
     * @return true如果应该记录，false如果不应该记录
     */
    public static boolean shouldLogOperation(String operation) {
        return operation != null && !IGNORED_OPERATIONS.contains(operation);
    }
} 