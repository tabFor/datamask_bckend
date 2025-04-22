package com.example.proxy;

import io.netty.buffer.ByteBuf;

/**
 * MySQL协议数据包
 * 表示一个完整的MySQL协议数据包
 */
public class MySQLPacket {
    
    private final int sequenceId;  // 包序号
    private final ByteBuf payload;  // 数据包内容
    private String sql;  // 解析出的SQL（如果是查询语句）
    private String maskedSql;  // 脱敏后的SQL
    
    /**
     * 构造函数
     *
     * @param sequenceId 包序号
     * @param payload 数据包内容
     */
    public MySQLPacket(int sequenceId, ByteBuf payload) {
        this.sequenceId = sequenceId;
        this.payload = payload;
    }
    
    /**
     * 获取包序号
     *
     * @return 包序号
     */
    public int getSequenceId() {
        return sequenceId;
    }
    
    /**
     * 获取数据包内容
     *
     * @return 数据包内容
     */
    public ByteBuf getPayload() {
        return payload;
    }
    
    /**
     * 获取解析出的SQL
     *
     * @return SQL语句
     */
    public String getSql() {
        return sql;
    }
    
    /**
     * 设置解析出的SQL
     *
     * @param sql SQL语句
     */
    public void setSql(String sql) {
        this.sql = sql;
    }
    
    /**
     * 获取脱敏后的SQL
     *
     * @return 脱敏后的SQL
     */
    public String getMaskedSql() {
        return maskedSql;
    }
    
    /**
     * 设置脱敏后的SQL
     *
     * @param maskedSql 脱敏后的SQL
     */
    public void setMaskedSql(String maskedSql) {
        this.maskedSql = maskedSql;
    }
    
    /**
     * 释放数据包资源
     */
    public void release() {
        if (payload != null && payload.refCnt() > 0) {
            payload.release();
        }
    }
    
    @Override
    public String toString() {
        return "MySQLPacket{" +
                "sequenceId=" + sequenceId +
                ", sql='" + sql + '\'' +
                ", maskedSql='" + maskedSql + '\'' +
                '}';
    }
} 