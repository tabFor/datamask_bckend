package com.example.controller;

import com.example.dto.ApiResponse;
import com.example.dto.DatabaseConnectionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/database")
@Tag(name = "数据库管理", description = "提供数据库连接、表结构查询和元数据获取等功能，支持多种数据库类型，包括MySQL、PostgreSQL、Oracle和SQL Server")
public class DatabaseController {

    /**
     * 获取数据库中的表列表
     */
    @Operation(
        summary = "获取数据库表列表", 
        description = "根据提供的数据库连接信息，获取指定数据库中的所有表列表。支持MySQL、PostgreSQL、Oracle和SQL Server等多种数据库类型。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取表列表", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误或数据库连接失败"
        )
    })
    @GetMapping("/tables")
    public ApiResponse<Map<String, Object>> getTables(
            @Parameter(description = "数据库类型，支持mysql、postgresql、oracle、sqlserver") @RequestParam String dbType,
            @Parameter(description = "数据库服务器主机地址") @RequestParam String host,
            @Parameter(description = "数据库服务端口号") @RequestParam String port,
            @Parameter(description = "数据库用户名") @RequestParam String username,
            @Parameter(description = "数据库密码") @RequestParam String password,
            @Parameter(description = "数据库名称") @RequestParam String dbName) {
        
        Connection connection = null;
        try {
            // 获取数据库连接
            connection = getDatabaseConnection(dbType, host, port, dbName, username, password);
            
            // 获取表列表
            List<String> tables = getTableList(connection, dbType, dbName, username);
            
            Map<String, Object> result = new HashMap<>();
            result.put("tables", tables);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取表列表失败: " + e.getMessage());
        } finally {
            closeConnection(connection);
        }
    }

    /**
     * 获取表的列信息
     */
    @Operation(
        summary = "获取表的列信息", 
        description = "根据提供的数据库连接信息和表名，获取指定表的所有列名和列类型。支持MySQL、PostgreSQL、Oracle和SQL Server等多种数据库类型。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取表列信息", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "请求参数错误、数据库连接失败或表不存在"
        )
    })
    @GetMapping("/columns")
    public ApiResponse<Map<String, Object>> getColumns(
            @Parameter(description = "数据库类型，支持mysql、postgresql、oracle、sqlserver") @RequestParam String dbType,
            @Parameter(description = "数据库服务器主机地址") @RequestParam String host,
            @Parameter(description = "数据库服务端口号") @RequestParam String port,
            @Parameter(description = "数据库用户名") @RequestParam String username,
            @Parameter(description = "数据库密码") @RequestParam String password,
            @Parameter(description = "数据库名称") @RequestParam String dbName,
            @Parameter(description = "表名") @RequestParam String tableName) {
        
        Connection connection = null;
        try {
            // 获取数据库连接
            connection = getDatabaseConnection(dbType, host, port, dbName, username, password);
            
            // 获取列信息
            List<String> columns = getColumnList(connection, dbType, tableName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("columns", columns);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取表字段失败: " + e.getMessage());
        } finally {
            closeConnection(connection);
        }
    }

    /**
     * 测试数据库连接
     */
    @Operation(
        summary = "测试数据库连接", 
        description = "根据提供的数据库连接信息，测试是否能成功连接到指定的数据库服务器。支持MySQL、PostgreSQL、Oracle和SQL Server等多种数据库类型。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "连接测试成功", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "连接测试失败，可能是连接参数错误或数据库服务不可用"
        )
    })
    @PostMapping("/test-connection")
    public ApiResponse<String> testConnection(
            @Parameter(description = "数据库连接参数，包括类型、主机、端口、数据库名、用户名和密码") 
            @RequestBody DatabaseConnectionDTO connectionDTO) {
        Connection connection = null;
        try {
            // 获取数据库连接
            connection = getDatabaseConnection(
                    connectionDTO.getDbType(),
                    connectionDTO.getHost(),
                    connectionDTO.getPort(),
                    connectionDTO.getDbName(),
                    connectionDTO.getUsername(),
                    connectionDTO.getPassword()
            );
            
            return ApiResponse.success("连接成功");
        } catch (Exception e) {
            return ApiResponse.error("连接失败: " + e.getMessage());
        } finally {
            closeConnection(connection);
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

    /**
     * 获取表列表
     */
    private List<String> getTableList(Connection connection, String dbType, String dbName, String username) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        ResultSet rs = null;
        try {
            String[] types = {"TABLE"};
            
            // 不同数据库可能需要不同的参数
            if ("mysql".equalsIgnoreCase(dbType) || "postgresql".equalsIgnoreCase(dbType)) {
                rs = metaData.getTables(dbName, null, "%", types);
            } else if ("oracle".equalsIgnoreCase(dbType)) {
                rs = metaData.getTables(null, username.toUpperCase(), "%", types);
            } else if ("sqlserver".equalsIgnoreCase(dbType)) {
                rs = metaData.getTables(dbName, "dbo", "%", types);
            } else {
                rs = metaData.getTables(null, null, "%", types);
            }
            
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
        
        return tables;
    }

    /**
     * 获取列列表
     */
    private List<String> getColumnList(Connection connection, String dbType, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            
            // 使用简单的查询获取列信息
            String query = "SELECT * FROM " + tableName + " WHERE 1=0";
            rs = stmt.executeQuery(query);
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }
        
        return columns;
    }

    /**
     * 关闭数据库连接
     */
    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // 忽略关闭连接时的异常
            }
        }
    }
} 