package com.example.interceptor;

import com.example.service.AuditLogService;
import com.example.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                // 从token中获取用户名
                String username = JwtUtil.extractUsername(token);
                
                // 获取请求方法和路径
                String method = request.getMethod();
                String path = request.getRequestURI();
                
                // 操作类型
                String operation = method + " " + path;
                
                // 检查操作是否需要记录
                if (com.example.controller.AuditLogController.shouldLogOperation(operation)) {
                    // 记录操作日志
                    auditLogService.createLog(
                        username,
                        operation,
                        "用户访问接口",
                        "成功"
                    );
                }
            } catch (Exception e) {
                // token无效，不记录日志
            }
        }
        return true;
    }
} 