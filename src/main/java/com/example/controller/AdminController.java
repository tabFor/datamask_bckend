package com.example.controller;

import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理员操作", description = "仅限管理员的高级操作")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Operation(
        summary = "密码迁移",
        description = "将数据库中所有明文密码转换为加密密码（仅限管理员）"
    )
    @ApiResponse(responseCode = "200", description = "迁移成功")
    @ApiResponse(responseCode = "403", description = "权限不足")
    @PostMapping("/migrate-passwords")
    public ResponseEntity<?> migratePasswords(@RequestHeader(value = "Authorization") String token) {
        // 验证管理员权限
        if (!hasAdminRole(token)) {
            return ResponseEntity.status(403).body(Map.of("message", "权限不足，需要管理员权限"));
        }
        
        List<User> users = userRepository.findAll();
        int migratedCount = 0;
        
        for (User user : users) {
            String password = user.getPassword();
            // 检查密码是否已经是BCrypt格式
            if (password != null && !password.isEmpty() && !password.startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                migratedCount++;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "密码迁移完成");
        response.put("migratedCount", migratedCount);
        
        return ResponseEntity.ok(response);
    }
    
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
} 