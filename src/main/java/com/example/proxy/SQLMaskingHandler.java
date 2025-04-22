package com.example.proxy;

import com.example.model.MaskingRuleEntity;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL脱敏处理器
 * 实现SQL查询的动态脱敏，并将请求转发到目标数据库
 */
public class SQLMaskingHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLMaskingHandler.class);
    
    private final String targetHost;
    private final int targetPort;
    private final String username;
    private final String password;
    private final Map<String, List<MaskingRuleEntity>> maskingRules;
    
    private Channel targetChannel;
    private Channel clientChannel;
    
    /**
     * 构造函数
     *
     * @param targetHost 目标数据库主机
     * @param targetPort 目标数据库端口
     * @param username 目标数据库用户名
     * @param password 目标数据库密码
     * @param maskingRules 脱敏规则
     */
    public SQLMaskingHandler(String targetHost, int targetPort, 
                            String username, String password, 
                            Map<String, List<MaskingRuleEntity>> maskingRules) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.username = username;
        this.password = password;
        this.maskingRules = maskingRules;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        clientChannel = ctx.channel();
        logger.info("客户端连接建立: {}", clientChannel.remoteAddress());
        
        // 连接到目标数据库
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                            new MySQLProtocolDecoder(),
                            new MySQLProtocolEncoder(),
                            new TargetChannelHandler(clientChannel)
                        );
                    }
                });
        
        ChannelFuture future = bootstrap.connect(targetHost, targetPort);
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                targetChannel = channelFuture.channel();
                logger.info("连接到目标数据库: {}:{}", targetHost, targetPort);
            } else {
                logger.error("连接目标数据库失败: {}:{}", targetHost, targetPort);
                clientChannel.close();
            }
        });
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (targetChannel == null || !targetChannel.isActive()) {
            logger.error("目标数据库连接不可用");
            return;
        }
        
        if (msg instanceof MySQLPacket) {
            MySQLPacket packet = (MySQLPacket) msg;
            String sql = packet.getSql();
            
            // 只处理查询SQL
            if (sql != null && !sql.isEmpty()) {
                logger.debug("收到SQL查询: {}", sql);
                
                // 解析SQL，提取表名
                String tableName = extractTableName(sql);
                if (tableName != null) {
                    // 查找该表的脱敏规则
                    List<MaskingRuleEntity> rules = maskingRules.get(tableName);
                    if (rules != null && !rules.isEmpty()) {
                        // 执行SQL脱敏
                        String maskedSql = maskSql(sql, rules);
                        if (!maskedSql.equals(sql)) {
                            logger.info("SQL脱敏完成: {} -> {}", sql, maskedSql);
                            packet.setMaskedSql(maskedSql);
                            
                            // 创建新的数据包，替换原始SQL
                            ByteBuf payload = packet.getPayload();
                            if (payload != null) {
                                // 保存原始的读索引
                                int originalIndex = payload.readerIndex();
                                
                                // 读取命令类型（第一个字节）
                                byte commandType = payload.readByte();
                                
                                // 创建新的载荷
                                ByteBuf newPayload = Unpooled.buffer();
                                newPayload.writeByte(commandType);  // 写入命令类型
                                newPayload.writeBytes(maskedSql.getBytes());  // 写入脱敏后的SQL
                                
                                // 创建新的MySQL数据包
                                MySQLPacket maskedPacket = new MySQLPacket(
                                    packet.getSequenceId(), newPayload);
                                
                                // 将新的数据包转发到目标数据库
                                targetChannel.writeAndFlush(maskedPacket);
                                
                                // 由于我们处理了数据包，释放原始数据包
                                packet.release();
                                return;
                            }
                        }
                    }
                }
            }
            
            // 如果没有进行脱敏，直接转发原始数据包
            targetChannel.writeAndFlush(packet);
        } else {
            // 非MySQL数据包，直接转发
            targetChannel.writeAndFlush(msg);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("客户端连接断开: {}", ctx.channel().remoteAddress());
        if (targetChannel != null && targetChannel.isActive()) {
            targetChannel.close();
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("处理SQL脱敏时发生异常: {}", cause.getMessage(), cause);
        ctx.close();
        if (targetChannel != null && targetChannel.isActive()) {
            targetChannel.close();
        }
    }
    
    /**
     * 解析SQL，提取表名
     *
     * @param sql SQL语句
     * @return 表名，如果无法提取则返回null
     */
    private String extractTableName(String sql) {
        try {
            // 简化的SQL解析，只支持基本的SELECT语句
            sql = sql.toLowerCase().trim();
            
            // 处理SELECT语句
            if (sql.startsWith("select")) {
                // 查找FROM子句
                Pattern pattern = Pattern.compile("\\sfrom\\s+(\\w+)");
                Matcher matcher = pattern.matcher(sql);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("解析SQL提取表名失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 根据脱敏规则对SQL进行脱敏
     *
     * @param sql 原始SQL
     * @param rules 脱敏规则
     * @return 脱敏后的SQL
     */
    private String maskSql(String sql, List<MaskingRuleEntity> rules) {
        String maskedSql = sql;
        
        // 应用每个脱敏规则
        for (MaskingRuleEntity rule : rules) {
            if (!rule.isActive()) {
                continue;
            }
            
            String columnName = rule.getColumnName();
            String maskingType = rule.getMaskingType();
            
            // 构建正则表达式，匹配列名
            Pattern pattern = Pattern.compile("\\b" + columnName + "\\b");
            Matcher matcher = pattern.matcher(maskedSql);
            
            // 根据脱敏类型替换列名
            switch (maskingType) {
                case "完全遮盖":
                    maskedSql = matcher.replaceAll("'******'");
                    break;
                case "部分遮盖":
                    maskedSql = matcher.replaceAll(
                        "CONCAT(SUBSTRING(" + columnName + ", 1, 3), '****', SUBSTRING(" + columnName + ", -4))");
                    break;
                case "替换":
                    maskedSql = matcher.replaceAll("'***'");
                    break;
                case "哈希":
                    maskedSql = matcher.replaceAll("MD5(" + columnName + ")");
                    break;
                case "随机化":
                    maskedSql = matcher.replaceAll("CONCAT('RAND_', FLOOR(RAND() * 1000))");
                    break;
                default:
                    logger.warn("未知的脱敏类型: {}", maskingType);
                    break;
            }
        }
        
        return maskedSql;
    }
    
    /**
     * 目标通道处理器
     * 处理从目标数据库返回的响应
     */
    private static class TargetChannelHandler extends ChannelInboundHandlerAdapter {
        
        private final Channel clientChannel;
        
        public TargetChannelHandler(Channel clientChannel) {
            this.clientChannel = clientChannel;
        }
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 将目标数据库的响应转发给客户端
            if (clientChannel.isActive()) {
                clientChannel.writeAndFlush(msg);
            } else {
                // 如果客户端连接已关闭，释放资源
                if (msg instanceof MySQLPacket) {
                    ((MySQLPacket) msg).release();
                }
            }
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.info("目标数据库连接断开");
            if (clientChannel.isActive()) {
                clientChannel.close();
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("处理目标数据库响应时发生异常: {}", cause.getMessage(), cause);
            ctx.close();
            if (clientChannel.isActive()) {
                clientChannel.close();
            }
        }
    }
} 