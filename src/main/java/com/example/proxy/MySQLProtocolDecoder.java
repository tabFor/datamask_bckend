package com.example.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MySQL协议解码器
 * 将二进制数据解码为MySQL数据包
 */
public class MySQLProtocolDecoder extends ByteToMessageDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(MySQLProtocolDecoder.class);
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 确保有足够的数据可读
        if (in.readableBytes() < 4) {
            return;
        }
        
        // 标记当前读取位置，以便在需要时回退
        in.markReaderIndex();
        
        // 读取MySQL数据包头
        int packetLength = in.readUnsignedMediumLE(); // 包长度（3字节）
        int packetSeq = in.readUnsignedByte();        // 包序号（1字节）
        
        // 检查是否有完整的数据包
        if (in.readableBytes() < packetLength) {
            // 数据不完整，重置读取位置并等待更多数据
            in.resetReaderIndex();
            return;
        }
        
        // 读取数据包内容
        ByteBuf payload = in.readRetainedSlice(packetLength);
        
        // 创建MySQL数据包对象并添加到输出列表
        MySQLPacket packet = new MySQLPacket(packetSeq, payload);
        
        // 解析SQL查询语句
        if (isSQLQuery(packet)) {
            try {
                String sql = extractSQLQuery(packet);
                packet.setSql(sql);
                logger.debug("解析到SQL: {}", sql);
            } catch (Exception e) {
                logger.error("解析SQL失败: {}", e.getMessage(), e);
            }
        }
        
        // 将解析后的数据包添加到输出列表
        out.add(packet);
    }
    
    /**
     * 判断数据包是否包含SQL查询
     *
     * @param packet MySQL数据包
     * @return 如果包含SQL查询返回true，否则返回false
     */
    private boolean isSQLQuery(MySQLPacket packet) {
        ByteBuf payload = packet.getPayload();
        if (payload != null && payload.readableBytes() > 0) {
            // 标记当前读取位置，以便在读取完成后重置
            payload.markReaderIndex();
            
            // 读取命令类型
            byte command = payload.readByte();
            
            // 重置读取位置
            payload.resetReaderIndex();
            
            // COM_QUERY命令类型为0x03
            return command == 0x03;
        }
        return false;
    }
    
    /**
     * 从COM_QUERY数据包中提取SQL查询语句
     *
     * @param packet MySQL数据包
     * @return SQL查询语句
     */
    private String extractSQLQuery(MySQLPacket packet) {
        ByteBuf payload = packet.getPayload();
        if (payload != null && payload.readableBytes() > 1) {
            // 标记当前读取位置，以便在读取完成后重置
            payload.markReaderIndex();
            
            // 跳过命令类型（1字节）
            payload.skipBytes(1);
            
            // 读取剩余的数据作为SQL查询
            byte[] sqlBytes = new byte[payload.readableBytes()];
            payload.readBytes(sqlBytes);
            
            // 重置读取位置
            payload.resetReaderIndex();
            
            // 将字节数组转换为字符串
            return new String(sqlBytes);
        }
        return "";
    }
} 