package com.example.dto;

import java.util.List;

public class MaskingRuleResponse {
    private List<MaskingRuleDTO> rules;
    private String message;
    private boolean success;

    public MaskingRuleResponse() {
    }

    public MaskingRuleResponse(List<MaskingRuleDTO> rules, String message, boolean success) {
        this.rules = rules;
        this.message = message;
        this.success = success;
    }

    public List<MaskingRuleDTO> getRules() {
        return rules;
    }

    public void setRules(List<MaskingRuleDTO> rules) {
        this.rules = rules;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
} 