package com.example.controller;

import com.example.service.MaskedDataService;
import com.example.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 脱敏数据查看控制器
 * 提供脱敏数据的查看、预览和下载功能
 * 仅管理员和数据分析师有权访问
 */
@RestController
@RequestMapping("/api/masked-data")
@Tag(name = "脱敏数据管理", description = "用于查看和下载脱敏后的数据")
public class MaskedDataViewController {

    private static final Logger logger = LoggerFactory.getLogger(MaskedDataViewController.class);

    @Autowired
    private MaskedDataService maskedDataService;

    @Operation(
        summary = "获取脱敏数据文件列表", 
        description = "获取所有可用的脱敏数据文件，包括文件名、大小、修改时间等信息"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功获取文件列表"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "权限不足，仅管理员和数据分析师可访问"
    )
    @GetMapping("/files")
    public ResponseEntity<?> listMaskedDataFiles(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        // 验证权限（只允许ADMIN和DATA_ANALYST角色访问）
        if (!hasViewPermission(token)) {
            logger.warn("未授权访问脱敏数据文件列表");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("无权限访问此资源"));
        }
        
        try {
            List<Map<String, Object>> files = maskedDataService.listMaskedFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("获取脱敏数据文件列表异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("获取脱敏数据文件列表失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "预览脱敏数据", 
        description = "在线预览脱敏数据内容，支持CSV和JSON格式的文件，并提供分页功能"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功预览数据"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "权限不足，仅管理员和数据分析师可访问"
    )
    @ApiResponse(
        responseCode = "404", 
        description = "文件不存在"
    )
    @GetMapping("/preview")
    public ResponseEntity<?> previewMaskedData(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "文件路径") @RequestParam String filePath,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "10") int size) {
        
        if (!hasViewPermission(token)) {
            logger.warn("未授权预览脱敏数据: {}", filePath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("无权限访问此资源"));
        }
        
        try {
            Map<String, Object> data = maskedDataService.previewMaskedData(filePath, page, size);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("预览脱敏数据异常: {}", filePath, e);
            HttpStatus status = e instanceof SecurityException ? HttpStatus.FORBIDDEN : 
                               (e instanceof FileNotFoundException ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(status).body(createErrorResponse("预览数据失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "下载脱敏数据文件", 
        description = "下载完整的脱敏数据文件，支持CSV和JSON格式"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功下载文件"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "权限不足，仅管理员和数据分析师可访问"
    )
    @ApiResponse(
        responseCode = "404", 
        description = "文件不存在"
    )
    @GetMapping("/download")
    public ResponseEntity<?> downloadMaskedData(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "文件路径") @RequestParam String filePath) {
        
        if (!hasViewPermission(token)) {
            logger.warn("未授权下载脱敏数据: {}", filePath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("无权限访问此资源"));
        }
        
        try {
            Resource resource = maskedDataService.getMaskedFileAsResource(filePath);
            String filename = resource.getFilename();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            logger.error("下载脱敏数据文件异常: {}", filePath, e);
            HttpStatus status = e instanceof SecurityException ? HttpStatus.FORBIDDEN : 
                               (e instanceof FileNotFoundException ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(status).body(createErrorResponse("下载文件失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "查询脱敏后的数据库表", 
        description = "查询数据库中的脱敏表数据，支持分页和条件查询"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功查询数据"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "权限不足，仅管理员和数据分析师可访问"
    )
    @GetMapping("/db-query")
    public ResponseEntity<?> queryMaskedDatabaseTable(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "表名") @RequestParam String tableName,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "查询条件（JSON格式）") @RequestParam(required = false) String conditions) {
        
        if (!hasViewPermission(token)) {
            logger.warn("未授权查询脱敏数据表: {}", tableName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("无权限访问此资源"));
        }
        
        try {
            Map<String, Object> result = maskedDataService.queryMaskedTable(tableName, page, size, conditions);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("查询脱敏数据表异常: {}", tableName, e);
            HttpStatus status = e instanceof SecurityException ? HttpStatus.FORBIDDEN : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(createErrorResponse("查询数据失败: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "获取可用的脱敏数据库表", 
        description = "获取数据库中所有可查询的脱敏表列表"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功获取表列表"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "权限不足，仅管理员和数据分析师可访问"
    )
    @GetMapping("/db-tables")
    public ResponseEntity<?> listMaskedDatabaseTables(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "是否强制刷新缓存") @RequestParam(required = false, defaultValue = "false") boolean forceRefresh) {
        
        if (!hasViewPermission(token)) {
            logger.warn("未授权访问脱敏数据表列表");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("无权限访问此资源"));
        }
        
        try {
            logger.info("开始获取脱敏数据表列表, 强制刷新: {}", forceRefresh);
            
            // 如果需要强制刷新，先清除缓存
            if (forceRefresh) {
                if (hasAdminPermission(token)) {
                    logger.info("管理员请求强制刷新脱敏数据表列表缓存");
                    maskedDataService.clearAllCaches();
                } else {
                    logger.warn("非管理员用户尝试强制刷新缓存，已忽略该请求");
                }
            }
            
            List<String> tables = maskedDataService.listMaskedTables();
            logger.info("成功获取到 {} 个脱敏数据表", tables.size());
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            String errorMessage = "获取脱敏数据表列表失败";
            
            // 记录详细错误信息，包括异常堆栈
            logger.error("{}: {}", errorMessage, e.getMessage(), e);
            
            // 返回更有用的错误信息给客户端
            Map<String, Object> errorResponse = createErrorResponse(errorMessage + ": " + e.getMessage());
            
            // 添加额外的调试信息（仅在开发环境中使用，生产环境应移除）
            errorResponse.put("exception", e.getClass().getName());
            errorResponse.put("stackTrace", Arrays.stream(e.getStackTrace())
                    .limit(10)  // 限制堆栈深度
                    .map(StackTraceElement::toString)
                    .collect(Collectors.toList()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Operation(
        summary = "更新脱敏数据", 
        description = "更新指定表的脱敏数据，会清除相关缓存"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功更新数据"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "权限不足，仅管理员可访问"
    )
    @PostMapping("/update/{tableName}")
    public ResponseEntity<?> updateMaskedData(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "表名") @PathVariable String tableName,
            @Parameter(description = "更新数据") @RequestBody Map<String, Object> data) {
        
        // 验证权限（只允许ADMIN角色访问）
        if (!hasAdminPermission(token)) {
            logger.warn("未授权更新脱敏数据: {}", tableName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("无权限更新数据"));
        }
        
        try {
            // 更新数据
            maskedDataService.updateMaskedData(tableName, data);
            
            // 清除相关缓存
            maskedDataService.clearTableCache(tableName);
            
            return ResponseEntity.ok(createSuccessResponse("数据更新成功"));
        } catch (Exception e) {
            logger.error("更新脱敏数据失败: {}", tableName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("更新数据失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "删除脱敏数据", 
        description = "删除指定表的脱敏数据，会清除相关缓存"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "成功删除数据"
    )
    @ApiResponse(
        responseCode = "403", 
        description = "权限不足，仅管理员可访问"
    )
    @DeleteMapping("/{tableName}")
    public ResponseEntity<?> deleteMaskedData(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "表名") @PathVariable String tableName) {
        
        // 验证权限（只允许ADMIN角色访问）
        if (!hasAdminPermission(token)) {
            logger.warn("未授权删除脱敏数据: {}", tableName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("无权限删除数据"));
        }
        
        try {
            // 删除数据
            maskedDataService.deleteMaskedData(tableName);
            
            // 清除相关缓存
            maskedDataService.clearTableCache(tableName);
            
            return ResponseEntity.ok(createSuccessResponse("数据删除成功"));
        } catch (Exception e) {
            logger.error("删除脱敏数据失败: {}", tableName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("删除数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查用户是否具有查看脱敏数据的权限
     */
    private boolean hasViewPermission(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                String role = JwtUtil.extractRole(jwtToken);
                
                // 只允许管理员和数据分析师访问
                return "ADMIN".equals(role) || "DATA_ANALYST".equals(role);
            }
            return false;
        } catch (Exception e) {
            logger.error("验证权限失败", e);
            return false;
        }
    }
    
    /**
     * 检查用户是否具有管理员权限
     */
    private boolean hasAdminPermission(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                String role = JwtUtil.extractRole(jwtToken);
                
                // 只允许管理员访问
                return "ADMIN".equals(role);
            }
            return false;
        } catch (Exception e) {
            logger.error("验证权限失败", e);
            return false;
        }
    }
    
    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
    
    /**
     * 文件不存在异常
     */
    private static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(String message) {
            super(message);
        }
    }
} 