package com.example.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "masking_rules")
public class MaskingRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "database_name", nullable = false)
    @JsonProperty("database")
    private String database;

    @Column(name = "table_name", nullable = false)
    @JsonProperty("tableName")
    private String tableName;

    @Column(name = "column_name", nullable = false)
    @JsonProperty("columnName")
    private String columnName;

    @Column(name = "masking_type", nullable = false)
    @JsonProperty("maskingType")
    private String maskingType;

    @Column(name = "active", nullable = false)
    @JsonProperty("active")
    private boolean active;

    public MaskingRuleEntity() {
    }

    public MaskingRuleEntity(String database, String tableName, String columnName, String maskingType, boolean active) {
        this.database = database;
        this.tableName = tableName;
        this.columnName = columnName;
        this.maskingType = maskingType;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getMaskingType() {
        return maskingType;
    }

    public void setMaskingType(String maskingType) {
        this.maskingType = maskingType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
} 