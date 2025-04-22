package com.example.dto;

public class MaskingRuleDTO {
    private Long id;
    private String ruleId;
    private String name;
    private String description;
    private String type;
    private String pattern;
    private Integer prefixLength;
    private Integer suffixLength;
    private String replacementChar;
    private String database;
    private String tableName;
    private String columnName;
    private String maskingType;
    private boolean active = true;

    public MaskingRuleDTO() {
    }

    public MaskingRuleDTO(Long id, String database, String tableName, String columnName, String maskingType, boolean active) {
        this.id = id;
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

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Integer getPrefixLength() {
        return prefixLength;
    }

    public void setPrefixLength(Integer prefixLength) {
        this.prefixLength = prefixLength;
    }

    public Integer getSuffixLength() {
        return suffixLength;
    }

    public void setSuffixLength(Integer suffixLength) {
        this.suffixLength = suffixLength;
    }

    public String getReplacementChar() {
        return replacementChar;
    }

    public void setReplacementChar(String replacementChar) {
        this.replacementChar = replacementChar;
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