package com.example.util;

import com.example.model.User;
import com.example.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用于迁移明文密码到加密密码的工具类
 */
@Component
public class PasswordMigrationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationUtil.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 在应用完全启动后执行密码迁移
     * 使用ApplicationReadyEvent确保所有Bean都已经完全初始化
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        try {
            logger.info("开始密码迁移过程...");
            migratePasswords();
        } catch (Exception e) {
            logger.error("密码迁移过程发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 系统启动时自动迁移明文密码到加密密码
     */
    public void migratePasswords() {
        try {
            List<User> users = userRepository.findAll();
            int migratedCount = 0;
            
            for (User user : users) {
                try {
                    String password = user.getPassword();
                    // 检查密码是否已经是BCrypt格式 ($2a$开头)
                    if (password != null && !password.isEmpty() && !password.startsWith("$2a$")) {
                        logger.info("正在加密用户 [{}] 的密码", user.getUsername());
                        user.setPassword(passwordEncoder.encode(password));
                        userRepository.save(user);
                        migratedCount++;
                    }
                } catch (Exception e) {
                    logger.error("处理用户 [{}] 密码时出错: {}", user.getUsername(), e.getMessage());
                }
            }
            
            logger.info("密码迁移完成，共迁移 {} 个用户密码", migratedCount);
        } catch (Exception e) {
            logger.error("获取用户列表时出错: {}", e.getMessage());
            throw e;
        }
    }
} 