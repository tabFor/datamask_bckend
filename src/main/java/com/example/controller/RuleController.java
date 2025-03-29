package com.example.controller;

import com.example.dto.ApiResponse;
import com.example.model.DesensitizationRule;
import com.example.service.DesensitizationRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "脱敏规则管理", description = "提供脱敏规则的创建、查询、更新和删除功能，支持多种数据类型的脱敏策略配置")
public class RuleController {

    @Autowired
    private DesensitizationRuleService ruleService;

    /**
     * 获取所有脱敏规则
     */
    @Operation(
        summary = "获取所有脱敏规则", 
        description = "返回系统中配置的所有数据脱敏规则，包括预设规则和用户自定义规则。这些规则定义了不同类型敏感数据的脱敏方式。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取规则列表", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping
    public ApiResponse<Map<String, Object>> getAllRules() {
        List<DesensitizationRule> rules = ruleService.getAllRules();
        Map<String, Object> result = new HashMap<>();
        result.put("rules", rules);
        return ApiResponse.success(result);
    }

    /**
     * 根据ID获取脱敏规则
     */
    @Operation(
        summary = "根据ID获取脱敏规则", 
        description = "根据规则ID获取特定脱敏规则的详细配置信息，包括名称、描述、适用数据类型、脱敏模式和参数等"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功获取规则详情"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定ID的规则不存在")
    })
    @GetMapping("/{id}")
    public ApiResponse<DesensitizationRule> getRuleById(
        @Parameter(description = "脱敏规则的唯一标识符") @PathVariable String id
    ) {
        // 使用 ruleId 获取规则
        Optional<DesensitizationRule> rule = ruleService.getRuleById(id);
        
        if (rule.isPresent()) {
            return ApiResponse.success(rule.get());
        } else {
            return ApiResponse.error("未找到ID为 " + id + " 的脱敏规则");
        }
    }

    /**
     * 创建自定义脱敏规则
     */
    @Operation(
        summary = "创建脱敏规则", 
        description = "创建新的数据脱敏规则。支持多种脱敏策略，如保留前后缀、全部替换、正则表达式替换等。创建后的规则可立即应用于数据脱敏任务。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "规则创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误或规则配置无效")
    })
    @PostMapping
    public ApiResponse<DesensitizationRule> createRule(
        @Parameter(description = "新脱敏规则的配置信息，包括名称、描述、类型、脱敏模式和参数等") 
        @RequestBody Map<String, Object> rule
    ) {
        try {
            // 在传递给服务层之前确保类型安全
            Map<String, Object> safeRule = new HashMap<>(rule);
            
            // 处理数值类型
            for (Map.Entry<String, Object> entry : rule.entrySet()) {
                if (entry.getValue() instanceof Integer) {
                    safeRule.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            // 确保 ruleId 字段的一致性
            if (rule.containsKey("id") && !rule.containsKey("ruleId")) {
                // 如果只有 id 没有 ruleId，则将 id 的值用于 ruleId
                safeRule.put("ruleId", rule.get("id").toString());
            }
            
            DesensitizationRule newRule = ruleService.createRule(safeRule);
            return ApiResponse.success(newRule);
        } catch (ClassCastException e) {
            return ApiResponse.error("创建规则失败：类型转换错误 - " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("创建规则失败：" + e.getMessage());
        }
    }

    /**
     * 更新脱敏规则
     */
    @Operation(
        summary = "更新脱敏规则", 
        description = "根据ID更新现有脱敏规则的配置。可以修改规则的名称、描述、脱敏模式和参数等属性，但不能更改规则的基本类型。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "规则更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定ID的规则不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误或规则配置无效")
    })
    @PutMapping("/{id}")
    public ApiResponse<DesensitizationRule> updateRule(
        @Parameter(description = "要更新的脱敏规则ID") @PathVariable String id, 
        @Parameter(description = "更新后的规则配置信息") @RequestBody Map<String, Object> updatedRule
    ) {
        // 确保使用 ruleId 进行查询和更新
        Optional<DesensitizationRule> rule = ruleService.updateRule(id, updatedRule);
        
        if (rule.isPresent()) {
            return ApiResponse.success(rule.get());
        } else {
            return ApiResponse.error("未找到ID为 " + id + " 的脱敏规则");
        }
    }

    /**
     * 删除脱敏规则
     */
    @Operation(
        summary = "删除脱敏规则", 
        description = "根据ID删除指定的脱敏规则。注意：如果该规则正在被使用（已关联到脱敏任务或字段映射），则可能无法删除。"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "规则删除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定ID的规则不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "规则正在使用中，无法删除")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRule(
        @Parameter(description = "要删除的脱敏规则ID") @PathVariable String id
    ) {
        // 使用 ruleId 删除规则
        boolean removed = ruleService.deleteRule(id);
        
        if (removed) {
            return ApiResponse.success("脱敏规则删除成功");
        } else {
            return ApiResponse.error("未找到ID为 " + id + " 的脱敏规则");
        }
    }
} 