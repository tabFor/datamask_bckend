package com.example.dto;

import java.util.List;

public class MaskingRuleRequest {
    private List<MaskingRuleDTO> rules;

    public MaskingRuleRequest() {
    }

    public MaskingRuleRequest(List<MaskingRuleDTO> rules) {
        this.rules = rules;
    }

    public List<MaskingRuleDTO> getRules() {
        return rules;
    }

    public void setRules(List<MaskingRuleDTO> rules) {
        this.rules = rules;
    }
} 