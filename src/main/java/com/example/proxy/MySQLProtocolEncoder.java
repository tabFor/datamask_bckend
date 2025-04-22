package com.example.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MySQL协议编码器
 * 将MySQL数据包编码为二进制数据
 */
public class MySQLProtocolEncoder extends MessageToByteEncoder<MySQLPacket> {
    
    private static final Logger logger = LoggerFactory.getLogger(MySQLProtocolEncoder.class);
    
    @Override
    protected void encode(ChannelHandlerContext ctx, MySQLPacket packet, ByteBuf out) {
        try {
            ByteBuf payload = packet.getPayload();
            if (payload == null) {
                logger.warn("编码时发现空的数据包载荷");
                return;
            }
            
            // 获取载荷长度
            int length = payload.readableBytes();
            
            // 写入MySQL数据包头
            out.writeMediumLE(length);         // 包长度（3字节）
            out.writeByte(packet.getSequenceId()); // 包序号（1字节）
            
            // 写入包内容
            out.writeBytes(payload, payload.readerIndex(), length);
            
            logger.debug("编码MySQL数据包完成: 长度={}, 序号={}", length, packet.getSequenceId());
        } catch (Exception e) {
            logger.error("编码MySQL数据包失败: {}", e.getMessage(), e);
        }
    }
} 