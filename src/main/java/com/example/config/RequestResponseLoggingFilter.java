package com.example.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

/**
 * 请求和响应日志记录过滤器
 * 用于记录HTTP请求和响应的详细信息
 */
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 跳过静态资源的日志记录
        if (isStaticResource(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 包装请求和响应以便能够多次读取内容
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 调用过滤器链
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            // 记录请求和响应信息
            long duration = System.currentTimeMillis() - startTime;
            logRequest(requestWrapper, request.getRemoteAddr());
            logResponse(responseWrapper, duration);
            
            // 复制响应内容回原始响应
            responseWrapper.copyBodyToResponse();
        }
    }
    
    private boolean isStaticResource(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/static/") || 
               path.contains("/css/") || 
               path.contains("/js/") || 
               path.contains("/images/") || 
               path.contains("/favicon.ico") ||
               path.contains("/swagger-ui/") ||
               path.contains("/v3/api-docs/");
    }
    
    private void logRequest(ContentCachingRequestWrapper request, String remoteAddr) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        // 获取请求头
        Enumeration<String> headerNames = request.getHeaderNames();
        String headers = Collections.list(headerNames)
                .stream()
                .filter(name -> !name.equalsIgnoreCase("authorization")) // 排除敏感头信息
                .map(name -> name + ":" + request.getHeader(name))
                .collect(Collectors.joining(", "));
        
        // 获取请求体
        String requestBody = getContentAsString(request.getContentAsByteArray(), request.getCharacterEncoding());
        
        logger.debug("=========================== HTTP 请求 ===========================");
        logger.debug("远程地址: {}", remoteAddr);
        logger.debug("请求方法: {} {}{}", method, uri, queryString != null ? "?" + queryString : "");
        logger.debug("请求头: {}", headers);
        logger.debug("请求体: {}", requestBody);
        logger.debug("================================================================");
    }
    
    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        
        // 获取响应头
        String headers = response.getHeaderNames()
                .stream()
                .map(name -> name + ":" + response.getHeader(name))
                .collect(Collectors.joining(", "));
        
        // 获取响应体
        String responseBody = getContentAsString(response.getContentAsByteArray(), response.getCharacterEncoding());
        
        logger.debug("=========================== HTTP 响应 ===========================");
        logger.debug("状态码: {}", status);
        logger.debug("耗时: {}ms", duration);
        logger.debug("响应头: {}", headers);
        logger.debug("响应体: {}", responseBody);
        logger.debug("================================================================");
    }
    
    private String getContentAsString(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return "";
        }
        
        try {
            return new String(content, encoding != null ? encoding : StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "[无法读取内容: " + e.getMessage() + "]";
        }
    }
} 