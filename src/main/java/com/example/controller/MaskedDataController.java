package com.example.controller;

import com.example.dto.ApiResponse;
import com.example.service.DataMaskingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 脱敏数据控制器
 * 用于返回已脱敏的SQL数据
 */
@RestController
@RequestMapping("/api/masked-data")
@Tag(name = "脱敏数据查询", description = "提供示例脱敏数据的查询接口，包括用户信息和订单信息的脱敏数据")
public class MaskedDataController {

    @Autowired
    private DataMaskingService dataMaskingService;

    /**
     * 获取脱敏后的用户数据
     * 
     * @return 脱敏后的用户数据列表
     */
    @Operation(
        summary = "获取脱敏后的用户数据", 
        description = "返回脱敏处理后的用户数据示例，包括用户ID、用户名、邮箱、手机号和身份证号等信息，敏感字段已经按照预设规则进行了脱敏处理"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取脱敏后的用户数据", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "权限不足，需要数据分析师或管理员权限"
        )
    })
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getMaskedUsers() {
        // 模拟从数据库获取的原始数据
        List<Map<String, Object>> originalUsers = getMockUserData();
        
        // 对数据进行脱敏处理
        List<Map<String, Object>> maskedUsers = new ArrayList<>();
        for (Map<String, Object> user : originalUsers) {
            Map<String, Object> maskedUser = new HashMap<>();
            
            // 保留不需要脱敏的字段
            maskedUser.put("id", user.get("id"));
            maskedUser.put("createTime", user.get("createTime"));
            maskedUser.put("updateTime", user.get("updateTime"));
            maskedUser.put("status", user.get("status"));
            
            // 对敏感字段进行脱敏
            if (user.get("username") != null) {
                maskedUser.put("username", dataMaskingService.maskValue(
                    (String) user.get("username"),
                    "username",
                    null,
                    1,
                    1,
                    "*"
                ));
            }
            
            if (user.get("email") != null) {
                maskedUser.put("email", dataMaskingService.maskValue(
                    (String) user.get("email"),
                    "email",
                    null,
                    1,
                    1,
                    "*"
                ));
            }
            
            if (user.get("phone") != null) {
                maskedUser.put("phone", dataMaskingService.maskValue(
                    (String) user.get("phone"),
                    "phone",
                    null,
                    3,
                    4,
                    "*"
                ));
            }
            
            if (user.get("idCard") != null) {
                maskedUser.put("idCard", dataMaskingService.maskValue(
                    (String) user.get("idCard"),
                    "idCard",
                    null,
                    6,
                    4,
                    "*"
                ));
            }
            
            if (user.get("address") != null) {
                maskedUser.put("address", dataMaskingService.maskValue(
                    (String) user.get("address"),
                    "address",
                    null,
                    5,
                    5,
                    "*"
                ));
            }
            
            maskedUsers.add(maskedUser);
        }
        
        return ApiResponse.success(maskedUsers);
    }
    
    /**
     * 获取脱敏后的订单数据
     * 
     * @return 脱敏后的订单数据列表
     */
    @Operation(
        summary = "获取脱敏后的订单数据", 
        description = "返回脱敏处理后的订单数据示例，包括订单ID、订单号、收货人信息、创建时间和订单金额等，敏感字段如收货人姓名、联系电话和地址已按照预设规则脱敏"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功获取脱敏后的订单数据", 
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "权限不足，需要数据分析师或管理员权限"
        )
    })
    @GetMapping("/orders")
    public ApiResponse<List<Map<String, Object>>> getMaskedOrders() {
        // 模拟从数据库获取的原始数据
        List<Map<String, Object>> originalOrders = getMockOrderData();
        
        // 对数据进行脱敏处理
        List<Map<String, Object>> maskedOrders = new ArrayList<>();
        for (Map<String, Object> order : originalOrders) {
            Map<String, Object> maskedOrder = new HashMap<>();
            
            // 保留不需要脱敏的字段
            maskedOrder.put("id", order.get("id"));
            maskedOrder.put("orderNo", order.get("orderNo"));
            maskedOrder.put("createTime", order.get("createTime"));
            maskedOrder.put("status", order.get("status"));
            maskedOrder.put("amount", order.get("amount"));
            
            // 对敏感字段进行脱敏
            if (order.get("receiverName") != null) {
                maskedOrder.put("receiverName", dataMaskingService.maskValue(
                    (String) order.get("receiverName"),
                    "username",
                    null,
                    1,
                    1,
                    "*"
                ));
            }
            
            if (order.get("receiverPhone") != null) {
                maskedOrder.put("receiverPhone", dataMaskingService.maskValue(
                    (String) order.get("receiverPhone"),
                    "phone",
                    null,
                    3,
                    4,
                    "*"
                ));
            }
            
            if (order.get("receiverAddress") != null) {
                maskedOrder.put("receiverAddress", dataMaskingService.maskValue(
                    (String) order.get("receiverAddress"),
                    "address",
                    null,
                    5,
                    5,
                    "*"
                ));
            }
            
            maskedOrders.add(maskedOrder);
        }
        
        return ApiResponse.success(maskedOrders);
    }
    
    /**
     * 模拟用户数据
     */
    private List<Map<String, Object>> getMockUserData() {
        List<Map<String, Object>> users = new ArrayList<>();
        
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1);
        user1.put("username", "zhangsan");
        user1.put("email", "zhangsan@example.com");
        user1.put("phone", "13800138001");
        user1.put("idCard", "320123199001010101");
        user1.put("address", "北京市朝阳区建国路88号");
        user1.put("createTime", "2023-01-01 10:00:00");
        user1.put("updateTime", "2023-01-10 15:30:00");
        user1.put("status", 1);
        users.add(user1);
        
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2);
        user2.put("username", "lisi");
        user2.put("email", "lisi@example.com");
        user2.put("phone", "13900139002");
        user2.put("idCard", "320123199002020202");
        user2.put("address", "上海市浦东新区陆家嘴1号");
        user2.put("createTime", "2023-02-01 09:30:00");
        user2.put("updateTime", "2023-02-15 14:20:00");
        user2.put("status", 1);
        users.add(user2);
        
        Map<String, Object> user3 = new HashMap<>();
        user3.put("id", 3);
        user3.put("username", "wangwu");
        user3.put("email", "wangwu@example.com");
        user3.put("phone", "13700137003");
        user3.put("idCard", "320123199003030303");
        user3.put("address", "广州市天河区体育西路100号");
        user3.put("createTime", "2023-03-01 11:20:00");
        user3.put("updateTime", "2023-03-20 16:40:00");
        user3.put("status", 0);
        users.add(user3);
        
        return users;
    }
    
    /**
     * 模拟订单数据
     */
    private List<Map<String, Object>> getMockOrderData() {
        List<Map<String, Object>> orders = new ArrayList<>();
        
        Map<String, Object> order1 = new HashMap<>();
        order1.put("id", 1);
        order1.put("orderNo", "ORD20230101001");
        order1.put("receiverName", "张三");
        order1.put("receiverPhone", "13800138001");
        order1.put("receiverAddress", "北京市朝阳区建国路88号2单元303室");
        order1.put("createTime", "2023-01-05 14:30:00");
        order1.put("status", "已完成");
        order1.put("amount", 299.50);
        orders.add(order1);
        
        Map<String, Object> order2 = new HashMap<>();
        order2.put("id", 2);
        order2.put("orderNo", "ORD20230215002");
        order2.put("receiverName", "李四");
        order2.put("receiverPhone", "13900139002");
        order2.put("receiverAddress", "上海市浦东新区陆家嘴1号3001室");
        order2.put("createTime", "2023-02-15 10:20:00");
        order2.put("status", "已发货");
        order2.put("amount", 599.90);
        orders.add(order2);
        
        Map<String, Object> order3 = new HashMap<>();
        order3.put("id", 3);
        order3.put("orderNo", "ORD20230320003");
        order3.put("receiverName", "王五");
        order3.put("receiverPhone", "13700137003");
        order3.put("receiverAddress", "广州市天河区体育西路100号1栋2201室");
        order3.put("createTime", "2023-03-20 09:15:00");
        order3.put("status", "待发货");
        order3.put("amount", 1299.00);
        orders.add(order3);
        
        return orders;
    }
} 