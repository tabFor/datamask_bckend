package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import com.example.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户增删改查相关接口")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @Operation(
        summary = "创建用户",
        description = "创建新用户，需要管理员权限"
    )
    @ApiResponse(responseCode = "201", description = "用户创建成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestHeader(value = "Authorization") String token,
            @RequestBody UserDTO userDTO) {
        
        try {
            // 验证权限
            if (!hasAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "权限不足"));
            }
            
            // 检查用户名是否已存在
            if (userService.findByUsername(userDTO.getUsername()) != null) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
            }
            
            // 创建新用户
            User newUser = new User();
            newUser.setUsername(userDTO.getUsername());
            newUser.setPassword(userDTO.getPassword());
            
            // 设置角色
            try {
                User.Role role = User.Role.valueOf(userDTO.getRole());
                newUser.setRole(role);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "无效的角色类型"));
            }
            
            User savedUser = userService.createUser(newUser);
            
            // 返回创建的用户信息（不包含密码）
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedUser.getId());
            response.put("username", savedUser.getUsername());
            response.put("role", savedUser.getRole().toString());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "创建用户失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "查询用户列表",
        description = "获取所有用户信息，需要管理员权限"
    )
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "Authorization") String token) {
        try {
            // 验证权限
            if (!hasAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "权限不足"));
            }
            
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users.stream().map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("role", user.getRole().toString());
                return userMap;
            }).toList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "查询用户失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "查询单个用户",
        description = "根据ID获取用户信息，需要管理员权限"
    )
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(
            @RequestHeader(value = "Authorization") String token,
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        
        try {
            // 验证权限
            if (!hasAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "权限不足"));
            }
            
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("role", user.getRole().toString());
                
                return ResponseEntity.ok(userMap);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "用户不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "查询用户失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "更新用户信息",
        description = "更新指定用户的信息，需要管理员权限"
    )
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(
            @RequestHeader(value = "Authorization") String token,
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody UserDTO userDTO) {
        
        try {
            // 验证权限
            if (!hasAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "权限不足"));
            }
            
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // 如果提供了新用户名且与现有用户不同，检查是否存在
                if (userDTO.getUsername() != null && !userDTO.getUsername().equals(user.getUsername())) {
                    User existingUser = userService.findByUsername(userDTO.getUsername());
                    if (existingUser != null) {
                        return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
                    }
                    user.setUsername(userDTO.getUsername());
                }
                
                // 更新密码（如果提供）
                if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
                    user.setPassword(userDTO.getPassword());
                }
                
                // 更新角色（如果提供）
                if (userDTO.getRole() != null) {
                    try {
                        User.Role role = User.Role.valueOf(userDTO.getRole());
                        user.setRole(role);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("message", "无效的角色类型"));
                    }
                }
                
                User updatedUser = userService.updateUser(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("id", updatedUser.getId());
                response.put("username", updatedUser.getUsername());
                response.put("role", updatedUser.getRole().toString());
                response.put("message", "用户更新成功");
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "用户不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "更新用户失败: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "删除用户",
        description = "删除指定用户，需要管理员权限"
    )
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader(value = "Authorization") String token,
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        
        try {
            // 验证权限
            if (!hasAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "权限不足"));
            }
            
            // 检查用户是否存在
            if (!userService.existsById(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "用户不存在"));
            }
            
            userService.deleteUser(userId);
            
            return ResponseEntity.ok(Map.of("message", "用户删除成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "删除用户失败: " + e.getMessage()));
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

    @Schema(description = "用户数据传输对象")
    public static class UserDTO {
        @Schema(description = "用户名", example = "newuser")
        private String username;
        
        @Schema(description = "密码", example = "password123")
        private String password;
        
        @Schema(description = "角色", example = "DATA_OPERATOR", allowableValues = {"ADMIN", "DATA_ANALYST", "DATA_OPERATOR"})
        private String role;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
} 