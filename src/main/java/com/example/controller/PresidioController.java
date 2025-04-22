package com.example.controller;

import com.example.model.SensitiveColumn;
import com.example.service.PresidioService;
import com.example.service.SensitiveDataDetector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presidio功能控制器
 */
@RestController
@RequestMapping("/api/presidio")
@Tag(name = "敏感数据识别", description = "基于Microsoft Presidio的敏感数据识别与脱敏API，支持自动分析和处理多种敏感信息类型")
public class PresidioController {

    @Autowired
    private PresidioService presidioService;
    
    @Autowired
    private SensitiveDataDetector sensitiveDataDetector;

    @Value("${presidio.analyzer.url:http://localhost:5001}")
    private String analyzerUrl;

    @Value("${presidio.anonymizer.url:http://localhost:5002}")
    private String anonymizerUrl;

    /**
     * 测试Presidio连接状态
     */
    @Operation(
        summary = "检查Presidio服务状态", 
        description = "检查Presidio分析器和匿名化器服务的连接状态，并返回简单的测试结果。用于监控和确认敏感数据处理服务是否正常运行。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功检查服务状态"
        )
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("analyzer_url", analyzerUrl);
        result.put("anonymizer_url", anonymizerUrl);
        
        boolean analyzerAvailable = false;
        boolean anonymizerAvailable = false;
        
        try {
            // 尝试分析一个简单文本作为测试
            List<Map<String, Object>> analyzeResult = presidioService.analyzeText("这是一个测试");
            analyzerAvailable = true;
            result.put("analyzer_available", true);
            result.put("analyzer_test", analyzeResult);
        } catch (Exception e) {
            result.put("analyzer_available", false);
            result.put("analyzer_error", e.getMessage());
        }
        
        try {
            // 尝试脱敏一个简单文本作为测试
            String anonymizedText = presidioService.processText("这是一个测试手机号13800138000");
            anonymizerAvailable = true;
            result.put("anonymizer_available", true);
            result.put("anonymizer_test", anonymizedText);
        } catch (Exception e) {
            result.put("anonymizer_available", false);
            result.put("anonymizer_error", e.getMessage());
        }
        
        result.put("status", analyzerAvailable && anonymizerAvailable ? "正常" : "异常");
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 测试Presidio文本分析
     */
    @Operation(
        summary = "分析文本中的敏感信息", 
        description = "使用Presidio分析器识别文本中的敏感信息，如个人身份信息、信用卡号、电话号码、地址等，返回识别结果及其位置和类型。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功分析文本"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，如文本为空"
        )
    })
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeText(
            @Parameter(description = "包含待分析文本的请求体") 
            @RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        
        if (text.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文本内容不能为空");
            return ResponseEntity.badRequest().body(error);
        }
        
        List<Map<String, Object>> results = presidioService.analyzeText(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("text", text);
        response.put("results", results);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试Presidio文本脱敏
     */
    @Operation(
        summary = "脱敏文本中的敏感信息", 
        description = "使用Presidio自动识别并脱敏文本中的敏感信息，适用于需要保护隐私数据但保留文本结构的场景。支持多种脱敏策略，如替换、掩码、删除等。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功脱敏文本"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，如文本为空"
        )
    })
    @PostMapping("/anonymize")
    public ResponseEntity<Map<String, Object>> anonymizeText(
            @Parameter(description = "包含待脱敏文本的请求体") 
            @RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        
        if (text.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文本内容不能为空");
            return ResponseEntity.badRequest().body(error);
        }
        
        String anonymizedText = presidioService.processText(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("original_text", text);
        response.put("anonymized_text", anonymizedText);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 使用Presidio自动识别敏感列
     */
    @Operation(
        summary = "自动识别数据库表中的敏感列", 
        description = "使用Presidio自动分析数据库表内容，识别可能包含敏感信息的列，如身份证号、手机号码、地址等。支持多种数据库类型，可用于数据治理和合规检查。"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功识别敏感列"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，如表名为空"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "服务器内部错误，如数据库连接失败"
        )
    })
    @PostMapping("/detect-sensitive-columns")
    public ResponseEntity<Map<String, Object>> detectSensitiveColumns(
            @Parameter(description = "数据库表分析请求，包含表名和数据库连接信息") 
            @RequestBody Map<String, String> request) {
        String tableName = request.get("tableName");
        String dbType = request.get("dbType");
        String host = request.get("host");
        String port = request.get("port");
        String username = request.get("username");
        String password = request.get("password");
        String dbName = request.get("dbName");
        
        if (tableName == null || tableName.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "表名不能为空");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            List<SensitiveColumn> sensitiveColumns;
            
            // 如果提供了数据库连接参数，则使用自定义连接
            if (dbType != null && host != null && port != null && 
                username != null && password != null && dbName != null) {
                
                // 创建数据库连接
                Connection connection = getDatabaseConnection(dbType, host, port, dbName, username, password);
                
                try {
                    // 使用自定义连接和Presidio检测敏感列
                    sensitiveColumns = sensitiveDataDetector.detectSensitiveColumnsWithPresidioAndConnection(connection, tableName);
                } finally {
                    // 关闭连接
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                }
            } else {
                // 使用默认连接
                sensitiveColumns = sensitiveDataDetector.detectSensitiveColumnsWithPresidio(tableName);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tableName", tableName);
            response.put("columns", sensitiveColumns);
            response.put("count", sensitiveColumns.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "识别敏感列失败: " + e.getMessage());
            error.put("error", e.toString());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 获取数据库连接
     */
    private Connection getDatabaseConnection(String dbType, String host, String port, String dbName, String username, String password) throws SQLException, ClassNotFoundException {
        String url;
        
        switch (dbType.toLowerCase()) {
            case "mysql":
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                Class.forName("com.mysql.cj.jdbc.Driver");
                break;
            case "postgresql":
                url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                Class.forName("org.postgresql.Driver");
                break;
            case "oracle":
                url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbName;
                Class.forName("oracle.jdbc.OracleDriver");
                break;
            case "sqlserver":
                url = "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + dbName;
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
        
        return DriverManager.getConnection(url, username, password);
    }
}