package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "desensitization_rules")
public class DesensitizationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", unique = true, nullable = false)
    private String ruleId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "pattern", nullable = false)
    private String pattern;

    @Column(name = "prefix_length")
    private Integer prefixLength;

    @Column(name = "suffix_length")
    private Integer suffixLength;

    @Column(name = "replacement_char")
    private String replacementChar;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public DesensitizationRule() {
    }

    public DesensitizationRule(String ruleId, String name, String description, String type, String pattern,
                              Integer prefixLength, Integer suffixLength, String replacementChar) {
        this.ruleId = ruleId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.pattern = pattern;
        this.prefixLength = prefixLength;
        this.suffixLength = suffixLength;
        this.replacementChar = replacementChar;
    }

    // Getters and Setters
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 