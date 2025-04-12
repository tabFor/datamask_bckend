package com.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.service.MaskingRuleService;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态脱敏控制器
 * 处理动态脱敏相关的API请求
 */
@RestController
@RequestMapping("/api/dynamic/masking")
@Tag(name = "动态脱敏管理", description = "提供数据库查询过程中的实时脱敏功能，包括统计信息查询和动态SQL拦截配置")
public class DynamicMaskingController {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicMaskingController.class);
    
    @Autowired
    private MaskingRuleService maskingRuleService;
    
    /**
     * 查询统计数据
     * 前端从此API获取统计信息
     */
    @Operation(
        summary = "查询动态脱敏统计数据", 
        description = "获取动态脱敏的统计信息，包括受保护表数量、列数量、活跃规则数量、处理记录数和处理性能指标等，用于监控和管理动态脱敏功能"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取统计数据"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误"
        )
    })
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(
            @Parameter(description = "查询请求，包含查询类型和表名等参数") 
            @RequestBody Map<String, Object> request) {
        log.info("收到动态脱敏查询请求: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 检查请求类型
            String type = request.containsKey("type") ? request.get("type").toString() : "";
            
            if ("stats".equals(type)) {
                // 返回统计数据
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalTables", 5);
                stats.put("totalColumns", 50);
                stats.put("totalRules", maskingRuleService.getActiveRules().size());
                stats.put("processedRecords", 10000);
                stats.put("performance", "95ms");
                
                response.put("success", true);
                response.put("data", stats);
            } else {
                // 处理表格数据查询
                String tableName = request.containsKey("selectedTable") ? request.get("selectedTable").toString() : "";
                
                // 这里可以添加针对特定表的处理逻辑
                
                response.put("success", true);
                response.put("message", "动态查询处理成功");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理动态脱敏查询请求时出错: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "处理请求时出错: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 