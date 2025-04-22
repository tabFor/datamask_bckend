package com.example.controller;

import com.example.model.SensitiveColumn;
import com.example.service.SensitiveDataDetector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensitive-data")
@Tag(name = "敏感数据检测", description = "提供敏感数据检测和识别功能，支持多种数据库类型")
public class SensitiveDataController {

    @Autowired
    private SensitiveDataDetector sensitiveDataDetector;

    @Operation(
        summary = "检测表的敏感列", 
        description = "检测指定表中的敏感列，支持自定义数据库连接参数或使用默认连接"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功检测到敏感列",
            content = @Content(schema = @Schema(implementation = SensitiveColumn.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "检测过程中发生错误"
        )
    })
    @GetMapping("/detect/{tableName}")
    public ResponseEntity<?> detectTableSensitiveColumns(
            @Parameter(description = "要检测的表名") @PathVariable String tableName,
            @Parameter(description = "数据库类型（mysql/postgresql/oracle/sqlserver）") @RequestParam(required = false) String dbType,
            @Parameter(description = "数据库主机地址") @RequestParam(required = false) String host,
            @Parameter(description = "数据库端口") @RequestParam(required = false) String port,
            @Parameter(description = "数据库用户名") @RequestParam(required = false) String username,
            @Parameter(description = "数据库密码") @RequestParam(required = false) String password,
            @Parameter(description = "数据库名称") @RequestParam(required = false) String dbName) {
        
        try {
            // 如果提供了数据库连接参数，则使用自定义连接
            if (dbType != null && host != null && port != null && 
                username != null && password != null && dbName != null) {
                
                // 创建数据库连接
                Connection connection = getDatabaseConnection(dbType, host, port, dbName, username, password);
                
                try {
                    // 使用自定义连接检测敏感列
                    List<SensitiveColumn> sensitiveColumns = 
                        sensitiveDataDetector.detectSensitiveColumnsWithConnection(connection, tableName);
                    return ResponseEntity.ok(sensitiveColumns);
                } finally {
                    // 关闭连接
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                }
            } else {
                // 使用默认连接
                List<SensitiveColumn> sensitiveColumns = sensitiveDataDetector.detectSensitiveColumns(tableName);
                return ResponseEntity.ok(sensitiveColumns);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "检测敏感列失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @Operation(
        summary = "检测所有敏感列", 
        description = "检测系统中所有表的敏感列"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功检测到所有敏感列",
            content = @Content(schema = @Schema(implementation = SensitiveColumn.class))
        )
    })
    @GetMapping("/detect")
    public ResponseEntity<List<SensitiveColumn>> detectAllSensitiveColumns() {
        List<SensitiveColumn> sensitiveColumns = sensitiveDataDetector.detectAllSensitiveColumns();
        return ResponseEntity.ok(sensitiveColumns);
    }

    @Operation(
        summary = "检测单个列的敏感度", 
        description = "根据列名和数据类型检测单个列的敏感度"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功检测到列的敏感度",
            content = @Content(schema = @Schema(implementation = SensitiveColumn.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数不完整"
        )
    })
    @PostMapping("/detect/column")
    public ResponseEntity<SensitiveColumn> detectColumn(
            @Parameter(description = "包含列名和数据类型的信息") @RequestBody Map<String, String> request) {
        String columnName = request.get("columnName");
        String dataType = request.get("dataType");
        
        if (columnName == null || dataType == null) {
            return ResponseEntity.badRequest().build();
        }
        
        SensitiveColumn sensitiveColumn = sensitiveDataDetector.detectSensitiveColumn(columnName, dataType);
        return ResponseEntity.ok(sensitiveColumn);
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