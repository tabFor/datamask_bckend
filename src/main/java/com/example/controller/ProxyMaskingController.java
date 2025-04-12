package com.example.controller;

import com.example.dto.ApiResponse;
import com.example.model.MaskingRuleEntity;
import com.example.proxy.DatabaseProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据库代理脱敏控制器
 * 用于管理代理服务器的脱敏规则
 */
@RestController
@RequestMapping("/api/proxy/masking")
public class ProxyMaskingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyMaskingController.class);
    
    private final DatabaseProxyManager proxyManager;
    
    @Autowired
    public ProxyMaskingController(DatabaseProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }
    
    /**
     * 更新表的脱敏规则
     *
     * @param tableName 表名
     * @param rules 脱敏规则列表
     * @return 响应结果
     */
    @PostMapping("/rules/{tableName}")
    public ApiResponse updateRules(@PathVariable String tableName, 
                                   @RequestBody List<MaskingRuleEntity> rules) {
        try {
            logger.info("更新表 {} 的脱敏规则，规则数量: {}", tableName, rules.size());
            proxyManager.updateMaskingRules(tableName, rules);
            return ApiResponse.success("脱敏规则更新成功");
        } catch (Exception e) {
            logger.error("更新脱敏规则失败: {}", e.getMessage(), e);
            return ApiResponse.error("更新脱敏规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除所有脱敏规则
     *
     * @return 响应结果
     */
    @DeleteMapping("/rules")
    public ApiResponse clearRules() {
        try {
            logger.info("清除所有脱敏规则");
            proxyManager.clearMaskingRules();
            return ApiResponse.success("所有脱敏规则已清除");
        } catch (Exception e) {
            logger.error("清除脱敏规则失败: {}", e.getMessage(), e);
            return ApiResponse.error("清除脱敏规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 返回代理服务器状态信息
     *
     * @return 响应结果
     */
    @GetMapping("/status")
    public ApiResponse getStatus() {
        try {
            return ApiResponse.success(proxyManager.getProxyStatus());
        } catch (Exception e) {
            logger.error("获取代理服务器状态失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取代理服务器状态fail: " + e.getMessage());
        }
    }
    
    /**
     * 启动代理服务器
     *
     * @return 响应结果
     */
    @PostMapping("/start")
    public ApiResponse startProxy() {
        try {
            boolean result = proxyManager.startProxy();
            if (result) {
                return ApiResponse.success("代理服务器启动成功");
            } else {
                return ApiResponse.error("代理服务器启动失败");
            }
        } catch (Exception e) {
            logger.error("启动代理服务器失败: {}", e.getMessage(), e);
            return ApiResponse.error("启动代理服务器失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止代理服务器
     *
     * @return 响应结果
     */
    @PostMapping("/stop")
    public ApiResponse stopProxy() {
        try {
            boolean result = proxyManager.stopProxy();
            if (result) {
                return ApiResponse.success("代理服务器停止成功");
            } else {
                return ApiResponse.error("代理服务器停止失败");
            }
        } catch (Exception e) {
            logger.error("停止代理服务器失败: {}", e.getMessage(), e);
            return ApiResponse.error("停止代理服务器失败: " + e.getMessage());
        }
    }
} 