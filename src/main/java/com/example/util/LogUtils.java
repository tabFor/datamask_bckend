package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类，用于统一管理控制台输出
 */
public class LogUtils {
    private static final Logger logger = LoggerFactory.getLogger(LogUtils.class);
    
    // 是否启用控制台调试输出
    private static boolean consoleOutputEnabled = false;
    
    /**
     * 输出调试信息
     */
    public static void debug(String message) {
        if (consoleOutputEnabled) {
            System.out.println(message);
        }
        logger.debug(message);
    }
    
    /**
     * 输出普通信息
     */
    public static void info(String message) {
        if (consoleOutputEnabled) {
            System.out.println(message);
        }
        logger.info(message);
    }
    
    /**
     * 输出警告信息
     */
    public static void warn(String message) {
        if (consoleOutputEnabled) {
            System.out.println("警告: " + message);
        }
        logger.warn(message);
    }
    
    /**
     * 输出错误信息
     */
    public static void error(String message) {
        if (consoleOutputEnabled) {
            System.err.println("错误: " + message);
        }
        logger.error(message);
    }
    
    /**
     * 输出错误信息和异常堆栈
     */
    public static void error(String message, Throwable throwable) {
        if (consoleOutputEnabled) {
            System.err.println("错误: " + message);
            throwable.printStackTrace();
        }
        logger.error(message, throwable);
    }
    
    /**
     * 设置是否启用控制台输出
     */
    public static void setConsoleOutputEnabled(boolean enabled) {
        consoleOutputEnabled = enabled;
    }
    
    /**
     * 获取当前控制台输出状态
     */
    public static boolean isConsoleOutputEnabled() {
        return consoleOutputEnabled;
    }
} 