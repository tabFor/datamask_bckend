package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Tag(name = "角色管理", description = "角色查询和分配相关接口")
public class RoleController {

    @Autowired
    private UserRepository userRepository;

    @Operation(
        summary = "查询所有角色",
        description = "获取系统中所有可用的角色"
    )
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/api/roles")
    public ResponseEntity<?> getAllRoles() {
        try {
            // 从枚举中获取所有角色
            List<Map<String, Object>> roles = Arrays.stream(User.Role.values())
                .map(role -> {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("id", role.ordinal());
                    roleMap.put("name", role.name());
                    roleMap.put("description", getDescription(role));
                    return roleMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "查询角色失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "查询角色详情",
        description = "获取指定角色的详细信息"
    )
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "角色不存在")
    @GetMapping("/api/roles/{roleId}")
    public ResponseEntity<?> getRoleById(@Parameter(description = "角色ID") @PathVariable int roleId) {
        try {
            if (roleId < 0 || roleId >= User.Role.values().length) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "角色不存在"));
            }
            
            User.Role role = User.Role.values()[roleId];
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", role.ordinal());
            roleMap.put("name", role.name());
            roleMap.put("description", getDescription(role));
            
            return ResponseEntity.ok(roleMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "查询角色失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "给用户分配角色",
        description = "为指定用户分配新角色，需要管理员权限"
    )
    @ApiResponse(responseCode = "200", description = "角色分配成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @ApiResponse(responseCode = "400", description = "无效的角色")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @PostMapping("/api/users/{userId}/roles")
    public ResponseEntity<?> assignRoleToUser(
            @RequestHeader(value = "Authorization") String token,
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody RoleAssignmentRequest request) {
        
        try {
            // 验证权限
            if (!hasAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "权限不足"));
            }
            
            // 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "用户不存在"));
            }
            
            User user = userOpt.get();
            
            // 验证角色有效性并设置
            try {
                User.Role role = User.Role.valueOf(request.getRole());
                user.setRole(role);
                userRepository.save(user);
                
                return ResponseEntity.ok(Map.of(
                    "message", "角色分配成功",
                    "username", user.getUsername(),
                    "role", user.getRole().toString()
                ));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "无效的角色类型"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "角色分配失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "移除用户角色",
        description = "移除指定用户的角色（重置为默认角色），需要管理员权限"
    )
    @ApiResponse(responseCode = "200", description = "角色移除成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @DeleteMapping("/api/users/{userId}/roles/{roleId}")
    public ResponseEntity<?> removeRoleFromUser(
            @RequestHeader(value = "Authorization") String token,
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "角色ID") @PathVariable int roleId) {
        
        try {
            // 验证权限
            if (!hasAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "权限不足"));
            }
            
            // 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "用户不存在"));
            }
            
            // 检查角色是否存在且是否分配给了用户
            if (roleId < 0 || roleId >= User.Role.values().length) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "角色不存在"));
            }
            
            User user = userOpt.get();
            User.Role targetRole = User.Role.values()[roleId];
            
            if (!user.getRole().equals(targetRole)) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户没有被分配该角色"));
            }
            
            // 重置为默认角色 (DATA_OPERATOR)
            user.setRole(User.Role.DATA_OPERATOR);
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "message", "角色移除成功，已重置为默认角色",
                "username", user.getUsername(),
                "role", user.getRole().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "角色移除失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "查询用户的所有角色",
        description = "获取指定用户的角色信息"
    )
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @GetMapping("/api/users/{userId}/roles")
    public ResponseEntity<?> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId) {
        try {
            // 检查用户是否存在
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "用户不存在"));
            }
            
            User user = userOpt.get();
            User.Role role = user.getRole();
            
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", role.ordinal());
            roleMap.put("name", role.name());
            roleMap.put("description", getDescription(role));
            
            // 由于当前系统一个用户只有一个角色，所以返回单一角色的信息
            // 如果后续支持一个用户多个角色，可以修改为返回列表
            return ResponseEntity.ok(roleMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "查询用户角色失败: " + e.getMessage()));
        }
    }

    // 辅助方法 - 获取角色描述
    private String getDescription(User.Role role) {
        switch (role) {
            case ADMIN:
                return "管理员";
            case DATA_ANALYST:
                return "数据分析师";
            case DATA_OPERATOR:
                return "数据操作员";
            default:
                return "未知角色";
        }
    }

    // 辅助方法 - 检查是否具有管理员权限
    private boolean hasAdminRole(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                String role = JwtUtil.extractRole(jwtToken);
                return "ADMIN".equals(role);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Schema(description = "角色分配请求参数")
    public static class RoleAssignmentRequest {
        @Schema(description = "角色名称", example = "ADMIN", allowableValues = {"ADMIN", "DATA_ANALYST", "DATA_OPERATOR"})
        private String role;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}