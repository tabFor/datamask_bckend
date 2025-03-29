package com.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String taskName;
    
    @Column(nullable = false)
    private String status;
    
    @Column
    private String sourceDatabase;
    
    @Column
    private String sourceTables;
    
    @Column(columnDefinition = "TEXT")
    private String maskingRules;
    
    @Column
    private String createdBy;
    
    @Column
    private LocalDateTime createTime;
    
    @Column
    private LocalDateTime updateTime;
    
    @Column
    private LocalDateTime executeTime;
    
    @Column
    private String taskDescription;
    
    @Column(columnDefinition = "TEXT")
    private String executionLog;
    
    @Column
    private String priority;
    
    @Column
    private String outputFormat;
    
    @Column
    private String outputLocation;
    
    @Column
    private String outputTable;
    
    @Column(columnDefinition = "TEXT")
    private String columnMappings;  // 存储列名和规则ID的映射关系，格式为JSON: {"column1": "ruleId1", "column2": "ruleId2"}

    // Getters and Setters
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getMaskingRules() {
        return maskingRules;
    }

    public void setMaskingRules(String maskingRules) {
        this.maskingRules = maskingRules;
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
    
    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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

    public String getOutputTable() {
        return outputTable;
    }

    public void setOutputTable(String outputTable) {
        this.outputTable = outputTable;
    }

    public String getColumnMappings() {
        return columnMappings;
    }

    public void setColumnMappings(String columnMappings) {
        this.columnMappings = columnMappings;
    }
} 