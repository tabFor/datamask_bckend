package com.example.filter;

import com.example.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // 对于OPTIONS请求（预检请求），直接放行
        if (request.getMethod().equals("OPTIONS")) {
            log.debug("OPTIONS请求，直接放行: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authorizationHeader = request.getHeader("Authorization");
        log.debug("处理请求: {} {}, Authorization头: {}", 
                request.getMethod(), requestURI, authorizationHeader != null ? "存在" : "不存在");

        String username = null;
        String jwt = null;
        String role = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = JwtUtil.extractUsername(jwt);
                role = JwtUtil.extractRole(jwt);
                log.debug("JWT验证成功，用户: {}, 角色: {}", username, role);
            } catch (Exception e) {
                log.error("JWT token验证失败: {}", e.getMessage());
            }
        } else {
            log.debug("请求没有有效的JWT token");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            log.debug("已为用户 {} 设置安全上下文认证", username);
        }

        log.debug("继续处理请求: {} {}", request.getMethod(), requestURI);
        filterChain.doFilter(request, response);
    }
} 