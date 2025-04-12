package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Presidio配置类
 */
@Configuration
public class PresidioConfig {

    /**
     * Presidio服务配置
     */
    @Bean
    @ConfigurationProperties(prefix = "presidio")
    public PresidioProperties presidioProperties() {
        return new PresidioProperties();
    }

    /**
     * 创建RestTemplate用于HTTP通信
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Presidio配置属性类
     */
    public static class PresidioProperties {
        private String analyzerUrl = "http://localhost:5001";
        private String anonymizerUrl = "http://localhost:5002";
        private boolean enabled = true;

        public String getAnalyzerUrl() {
            return analyzerUrl;
        }

        public void setAnalyzerUrl(String analyzerUrl) {
            this.analyzerUrl = analyzerUrl;
        }

        public String getAnonymizerUrl() {
            return anonymizerUrl;
        }

        public void setAnonymizerUrl(String anonymizerUrl) {
            this.anonymizerUrl = anonymizerUrl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
} 