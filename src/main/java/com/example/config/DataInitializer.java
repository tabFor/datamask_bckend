package com.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据初始化器
 * 用于在应用启动时执行SQL脚本，初始化测试数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // 临时关闭外键约束检查
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            // 移除数据库表中的masking_mode列
            executeScript("db/remove-masking-mode.sql");
            
            // 添加自动识别敏感列相关字段
            executeScript("db/add-auto-detect-columns.sql");
            
            // 初始化测试数据
            executeScript("db/init-test-data.sql");
            
            // 初始化用户数据
            executeScript("db/init-users.sql");
            
            // 重新启用外键约束检查
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            logger.info("所有数据初始化完成！");
        } catch (Exception e) {
            logger.error("数据初始化过程中发生错误", e);
            throw e;
        }
    }
    
    private void executeScript(String scriptPath) {
        try {
            logger.info("正在执行SQL脚本: {}", scriptPath);
            
            // 读取SQL脚本文件
            ClassPathResource resource = new ClassPathResource(scriptPath);
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            // 按分号分割SQL语句并执行
            String[] statements = sql.split(";");
            for (String statement : statements) {
                if (!statement.trim().isEmpty()) {
                    try {
                        jdbcTemplate.execute(statement);
                    } catch (Exception e) {
                        logger.error("执行SQL语句时出错: {}", statement, e);
                        // 继续执行下一条语句，而不是中断整个过程
                    }
                }
            }
            
            logger.info("SQL脚本 {} 执行完成", scriptPath);
        } catch (Exception e) {
            logger.error("执行SQL脚本 {} 时出错", scriptPath, e);
            throw new RuntimeException("执行SQL脚本时出错", e);
        }
    }
} 