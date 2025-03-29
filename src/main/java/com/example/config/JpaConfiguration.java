package com.example.config;

import com.example.interceptor.SQLMaskingInterceptor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class JpaConfiguration {

    private static final List<String> STATIC_MASKING_PATHS = Arrays.asList(
        "/api/tasks",
        "/api/desensitization",
        "/api/static-masking"
    );

    private final SQLMaskingInterceptor sqlMaskingInterceptor;

    @Autowired
    public JpaConfiguration(SQLMaskingInterceptor sqlMaskingInterceptor) {
        this.sqlMaskingInterceptor = sqlMaskingInterceptor;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> 
            hibernateProperties.put("hibernate.session_factory.statement_inspector", 
                new ContextAwareStatementInspector(sqlMaskingInterceptor));
    }
    
    /**
     * 上下文感知的StatementInspector，可以根据当前请求路径决定是否应用动态脱敏
     */
    private static class ContextAwareStatementInspector implements StatementInspector {
        
        private final SQLMaskingInterceptor delegate;
        
        public ContextAwareStatementInspector(SQLMaskingInterceptor delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String inspect(String sql) {
            // 获取当前请求路径
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            // 如果无法获取请求上下文，默认应用动态脱敏
            if (attributes == null) {
                return delegate.inspect(sql);
            }
            
            String requestPath = attributes.getRequest().getRequestURI();
            
            // 检查当前请求是否是静态脱敏API
            boolean isStaticMaskingApi = STATIC_MASKING_PATHS.stream()
                .anyMatch(requestPath::startsWith);
            
            // 如果是静态脱敏API，不应用动态脱敏，直接返回原始SQL
            if (isStaticMaskingApi) {
                return sql;
            }
            
            // 否则，应用动态脱敏
            return delegate.inspect(sql);
        }
    }
} 