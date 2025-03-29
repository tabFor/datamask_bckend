package com.example.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDTO {
    private Long id;
    private String taskName;
    private String description;
    private String priority;
    private String status;
    
    // 数据库配置
    private String dbType;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String dbName;
    private String tableName;
    
    // 脱敏规则
    private List<MaskingRuleDTO> maskingRules;
    
    // 列名与规则ID的映射关系
    private Map<String, String> columnMappings;  // 格式为: {"phone": "phone_mask", "id_card": "id_card_mask"}
    
    // 输出配置
    private String outputFormat; // CSV、JSON或DATABASE
    private String outputLocation; // CSV或JSON输出路径
    private String outputDatabase; // 输出的数据库名，可为空表示同库
    private String outputTable; // 输出的表名，若与源表相同则为同库同表覆盖脱敏
    
    // 执行计划
    private LocalDateTime planExecuteTime;
    
    // 原始字段
    private String sourceDatabase;
    private String sourceTables;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime executeTime;
    private String taskDescription;
    private String executionLog;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<MaskingRuleDTO> getMaskingRules() {
        return maskingRules;
    }

    public void setMaskingRules(List<MaskingRuleDTO> maskingRules) {
        this.maskingRules = maskingRules;
    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getOutputLocation() {
        return outputLocation;
    }

    public void setOutputLocation(String outputLocation) {
        this.outputLocation = outputLocation;
    }

    public String getOutputDatabase() {
        return outputDatabase;
    }

    public void setOutputDatabase(String outputDatabase) {
        this.outputDatabase = outputDatabase;
    }

    public String getOutputTable() {
        return outputTable;
    }

    public void setOutputTable(String outputTable) {
        this.outputTable = outputTable;
    }

    public LocalDateTime getPlanExecuteTime() {
        return planExecuteTime;
    }

    public void setPlanExecuteTime(LocalDateTime planExecuteTime) {
        this.planExecuteTime = planExecuteTime;
    }

    public String getSourceDatabase() {
        return sourceDatabase;
    }

    public void setSourceDatabase(String sourceDatabase) {
        this.sourceDatabase = sourceDatabase;
    }

    public String getSourceTables() {
        return sourceTables;
    }

    public void setSourceTables(String sourceTables) {
        this.sourceTables = sourceTables;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(LocalDateTime executeTime) {
        this.executeTime = executeTime;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }
} 