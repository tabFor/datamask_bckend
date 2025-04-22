package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.example.model.User;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import com.example.util.JwtUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@Tag(name = "认证管理", description = "用户登录和身份验证相关接口")
public class LoginController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(
        summary = "用户登录",
        description = "提供用户名和密码进行登录，返回包含角色信息的JWT令牌"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "登录成功或失败的结果，成功时返回token和用户角色信息"
    )
    @PostMapping("/api/login")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();
        User user = userService.findByUsername(loginRequest.getUsername());
        
        if (user != null) {
            boolean isAuthenticated = false;
            
            // 尝试验证密码
            if (userService.verifyPassword(loginRequest.getPassword(), user.getPassword())) {
                isAuthenticated = true;
            } 
            // 临时兼容：尝试明文密码匹配（如果不是加密格式）
            else if (!user.getPassword().startsWith("$2a$") && loginRequest.getPassword().equals(user.getPassword())) {
                isAuthenticated = true;
                // 自动加密明文密码
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepository.save(user);
            }
            
            if (isAuthenticated) {
                String token = JwtUtil.generateToken(user.getUsername(), user.getRole().toString());
                response.put("message", "登录成功");
                response.put("status", "success");
                response.put("token", token);
                response.put("role", user.getRole().toString());
                response.put("username", user.getUsername());
                return response;
            }
        }
        
        response.put("message", "登录失败");
        response.put("status", "failure");
        return response;
    }

    @Operation(
        summary = "用户登出",
        description = "用户登出系统"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "登出成功"
    )
    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        // 由于JWT是无状态的，服务端没有实际的登出操作
        // 正式实现可以考虑使用Redis缓存维护一个失效token列表，或使用较短的token有效期
        response.put("message", "登出成功");
        response.put("status", "success");
        return response;
    }

    @Operation(
        summary = "检查登录状态",
        description = "验证JWT令牌是否有效，返回用户的登录状态和角色信息"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "返回用户的登录状态、用户名和角色信息"
    )
    @GetMapping("/api/check-auth")
    public Map<String, Object> checkLogin(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                Claims claims = JwtUtil.validateTokenAndGetClaims(jwtToken);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                
                if (username != null) {
                    response.put("isLoggedIn", true);
                    response.put("username", username);
                    response.put("role", role);
                    return response;
                }
            }
            response.put("isLoggedIn", false);
            return response;
        } catch (Exception e) {
            response.put("isLoggedIn", false);
            return response;
        }
    }

    @Schema(description = "登录请求参数")
    public static class LoginRequest {
        @Schema(description = "用户名", example = "admin")
        private String username;
        
        @Schema(description = "密码", example = "admin123")
        private String password;

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
    }
}