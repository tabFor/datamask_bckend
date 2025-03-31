package com.example.config;

import com.example.util.LogUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
public class LoggingConfig {

    @Value("${logging.console.output.enabled:false}")
    private boolean consoleOutputEnabled;
    
    private final Environment environment;

    public LoggingConfig(Environment environment) {
        this.environment = environment;
    }
    
    @PostConstruct
    public void init() {
        // 设置是否启用控制台输出
        LogUtils.setConsoleOutputEnabled(consoleOutputEnabled);
        
        // 输出初始化信息
        LogUtils.info("日志系统初始化完成，控制台输出状态: " + 
                (LogUtils.isConsoleOutputEnabled() ? "已启用" : "已禁用"));
    }
} 