package com.example.proxy;

import com.example.model.MaskingRuleEntity;
import com.example.util.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库代理服务器
 * 使用Netty实现MySQL协议代理，支持动态数据脱敏
 */
public class ProxyServer {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    
    private final int proxyPort;
    private final String targetHost;
    private final int targetPort;
    private final String username;
    private final String password;
    private Map<String, List<MaskingRuleEntity>> maskingRules;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannel;
    
    /**
     * 构造函数
     *
     * @param proxyPort 代理服务器监听端口
     * @param targetHost 目标数据库主机
     * @param targetPort 目标数据库端口
     * @param username 目标数据库用户名
     * @param password 目标数据库密码
     * @param maskingRules 脱敏规则
     */
    public ProxyServer(int proxyPort, String targetHost, int targetPort, 
                       String username, String password, 
                       Map<String, List<MaskingRuleEntity>> maskingRules) {
        this.proxyPort = proxyPort;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.username = username;
        this.password = password;
        this.maskingRules = maskingRules != null ? maskingRules : new ConcurrentHashMap<>();
    }
    
    /**
     * 启动代理服务器
     * 
     * @throws Exception 启动异常
     */
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             // 添加MySQL协议处理器
                             ch.pipeline().addLast(
                                 new MySQLProtocolDecoder(),
                                 new MySQLProtocolEncoder(),
                                 new SQLMaskingHandler(targetHost, targetPort, 
                                                      username, password, maskingRules)
                             );
                         }
                     })
                     .option(ChannelOption.SO_BACKLOG, 128)
                     .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // 绑定端口并启动
            serverChannel = bootstrap.bind(proxyPort).sync();
            logger.info("数据库代理服务器已启动, 监听端口: {}", proxyPort);
            
            // 等待服务器关闭
            serverChannel.channel().closeFuture().sync();
        } finally {
            // 出现异常时关闭
            if (serverChannel == null || !serverChannel.channel().isActive()) {
                stop();
            }
        }
    }
    
    /**
     * 停止代理服务器
     * 
     * @throws Exception 停止异常
     */
    public void stop() throws Exception {
        logger.info("正在停止数据库代理服务器...");
        if (serverChannel != null) {
            serverChannel.channel().close().sync();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        logger.info("数据库代理服务器已停止");
    }
    
    /**
     * 设置脱敏规则
     *
     * @param maskingRules 脱敏规则
     */
    public void setMaskingRules(Map<String, List<MaskingRuleEntity>> maskingRules) {
        this.maskingRules = maskingRules;
    }
} 