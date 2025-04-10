package com.example.model;

import lombok.Data;

@Data
public class MaskingFact {
    private String value;
    private String type;
    private String pattern;
    private Integer prefixLength;
    private Integer suffixLength;
    private String replacementChar;
    private String maskedValue;
    
    public MaskingFact(String value, String type, String pattern, Integer prefixLength, Integer suffixLength, String replacementChar) {
        this.value = value;
        this.type = type;
        this.pattern = pattern;
        this.prefixLength = prefixLength;
        this.suffixLength = suffixLength;
        this.replacementChar = replacementChar;
        this.maskedValue = value;
    }

    public String getMaskedValue() {
        return maskedValue;
    }

    public void setMaskedValue(String maskedValue) {
        this.maskedValue = maskedValue;
    }
} 