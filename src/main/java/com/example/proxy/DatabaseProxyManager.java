package com.example.proxy;

import com.example.model.MaskingRuleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库代理管理器
 * 负责创建和管理数据库代理服务
 */
@Component
public class DatabaseProxyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseProxyManager.class);
    
    @Value("${proxy.server.port:3307}")
    private int proxyPort;
    
    @Value("${spring.datasource.url}")
    private String targetDatabaseUrl;
    
    @Value("${spring.datasource.username}")
    private String targetDatabaseUsername;
    
    @Value("${spring.datasource.password}")
    private String targetDatabasePassword;
    
    @Value("${proxy.auto.start:false}")
    private boolean autoStart;
    
    private final Map<String, List<MaskingRuleEntity>> maskingRules = new ConcurrentHashMap<>();
    private ProxyServer proxyServer;
    
    // 添加代理服务器状态标志
    private boolean running = false;
    
    @PostConstruct
    public void init() {
        // 根据配置决定是否自动启动代理
        if (autoStart) {
            startProxy();
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (proxyServer != null) {
            try {
                proxyServer.stop();
                logger.info("数据库代理服务已停止");
            } catch (Exception e) {
                logger.error("停止代理服务器失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 更新脱敏规则
     *
     * @param tableName 表名
     * @param rules 脱敏规则列表
     */
    public void updateMaskingRules(String tableName, List<MaskingRuleEntity> rules) {
        maskingRules.put(tableName, rules);
        if (proxyServer != null) {
            proxyServer.setMaskingRules(maskingRules);
        }
    }
    
    /**
     * 清除所有脱敏规则
     */
    public void clearMaskingRules() {
        maskingRules.clear();
        if (proxyServer != null) {
            proxyServer.setMaskingRules(maskingRules);
        }
    }
    
    /**
     * 从数据库URL中解析主机名和端口
     *
     * @param url 数据库URL
     * @return 数据库信息对象
     */
    private DatabaseInfo parseDatabaseUrl(String url) {
        // 示例URL: jdbc:mysql://localhost:3306/dbname
        String cleanUrl = url.substring(url.indexOf("://") + 3);
        String host = cleanUrl.substring(0, cleanUrl.indexOf(":"));
        String portStr = cleanUrl.substring(cleanUrl.indexOf(":") + 1, cleanUrl.indexOf("/"));
        int port = Integer.parseInt(portStr);
        
        return new DatabaseInfo(host, port);
    }
    
    /**
     * 数据库信息内部类
     */
    private static class DatabaseInfo {
        private final String host;
        private final int port;
        
        public DatabaseInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        public String getHost() {
            return host;
        }
        
        public int getPort() {
            return port;
        }
    }
    
    /**
     * 手动启动代理服务器
     * 
     * @return 操作是否成功
     */
    public boolean startProxy() {
        if (running) {
            logger.info("代理服务器已经在运行中");
            return true;
        }
        
        try {
            // 解析目标数据库URL
            DatabaseInfo dbInfo = parseDatabaseUrl(targetDatabaseUrl);
            
            // 创建并启动代理服务器
            proxyServer = new ProxyServer(proxyPort, dbInfo.getHost(), dbInfo.getPort(), 
                    targetDatabaseUsername, targetDatabasePassword, maskingRules);
            
            // 启动代理服务器
            new Thread(() -> {
                try {
                    proxyServer.start();
                } catch (Exception e) {
                    logger.error("代理服务器启动失败: {}", e.getMessage(), e);
                    running = false;
                }
            }).start();
            
            logger.info("数据库代理服务已启动，监听端口: {}", proxyPort);
            logger.info("代理目标: {}:{}", dbInfo.getHost(), dbInfo.getPort());
            running = true;
            return true;
        } catch (Exception e) {
            logger.error("启动数据库代理服务失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 手动停止代理服务器
     * 
     * @return 操作是否成功
     */
    public boolean stopProxy() {
        if (!running || proxyServer == null) {
            logger.info("代理服务器未在运行");
            return true;
        }
        
        try {
            proxyServer.stop();
            proxyServer = null;
            running = false;
            logger.info("数据库代理服务已停止");
            return true;
        } catch (Exception e) {
            logger.error("停止代理服务器失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取代理服务器状态
     * 
     * @return 代理服务器状态信息
     */
    public ProxyStatus getProxyStatus() {
        return new ProxyStatus(
            running,
            proxyPort,
            targetDatabaseUrl,
            maskingRules.size()
        );
    }
    
    /**
     * 代理状态信息类
     */
    public static class ProxyStatus {
        private final boolean running;
        private final int port;
        private final String targetDb;
        private final int rulesCount;
        
        public ProxyStatus(boolean running, int port, String targetDb, int rulesCount) {
            this.running = running;
            this.port = port;
            this.targetDb = targetDb;
            this.rulesCount = rulesCount;
        }
        
        public boolean isRunning() {
            return running;
        }
        
        public int getPort() {
            return port;
        }
        
        public String getTargetDb() {
            return targetDb;
        }
        
        public int getRulesCount() {
            return rulesCount;
        }
    }
} 