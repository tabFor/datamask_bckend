package com.example.config;

import com.example.interceptor.SQLMaskingInterceptor;
import com.example.interceptor.AuditLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SQLMaskingInterceptor sqlMaskingInterceptor;

    @Autowired
    private AuditLogInterceptor auditLogInterceptor;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")  // 适用于所有API路径
                        .allowedOrigins("http://localhost:8080") // 允许前端应用的源
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)  // 允许发送凭证信息
                        .maxAge(3600);  // 预检请求的有效期，单位为秒
            }
            
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // 排除静态脱敏相关的API路径
                registry.addInterceptor(new SqlStatementInterceptor(sqlMaskingInterceptor))
                       .excludePathPatterns("/api/tasks/**")  // 排除TaskController的API路径
                       .excludePathPatterns("/api/desensitization/**")  // 排除静态脱敏规则相关API
                       .excludePathPatterns("/api/static-masking/**");  // 排除其他静态脱敏API
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLogInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有/api/开头的请求
                .excludePathPatterns("/login", "/api/check-login");  // 排除登录相关的接口
    }
}

// SQL语句拦截器，用于包装SQLMaskingInterceptor
class SqlStatementInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
    
    private final SQLMaskingInterceptor sqlMaskingInterceptor;
    
    public SqlStatementInterceptor(SQLMaskingInterceptor sqlMaskingInterceptor) {
        this.sqlMaskingInterceptor = sqlMaskingInterceptor;
    }
    
    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, 
                             jakarta.servlet.http.HttpServletResponse response, 
                             Object handler) {
        // 在请求处理前启用SQL拦截
        return true;
    }
    
    @Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request, 
                                jakarta.servlet.http.HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 请求处理完成后的清理工作
    }
} 