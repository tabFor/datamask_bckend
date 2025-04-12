package com.example.service;

import com.example.model.SensitiveColumn;
import java.sql.Connection;
import java.util.List;

public interface SensitiveDataDetector {
    /**
     * 检测数据库表中的敏感列
     * @param tableName 表名
     * @return 敏感列列表
     */
    List<SensitiveColumn> detectSensitiveColumns(String tableName);

    /**
     * 检测数据库中的所有敏感列
     * @return 敏感列列表
     */
    List<SensitiveColumn> detectAllSensitiveColumns();

    /**
     * 根据列名和数据类型判断是否为敏感列
     * @param columnName 列名
     * @param dataType 数据类型
     * @return 敏感列信息，如果不是敏感列则返回null
     */
    SensitiveColumn detectSensitiveColumn(String columnName, String dataType);
    
    /**
     * 使用Presidio自动识别敏感列
     * @param tableName 表名
     * @return 敏感列列表
     */
    List<SensitiveColumn> detectSensitiveColumnsWithPresidio(String tableName);
    
    /**
     * 使用自定义数据库连接检测敏感列
     * @param connection 数据库连接
     * @param tableName 表名
     * @return 敏感列列表
     */
    List<SensitiveColumn> detectSensitiveColumnsWithConnection(Connection connection, String tableName);
    
    /**
     * 使用自定义数据库连接和Presidio检测敏感列
     * @param connection 数据库连接
     * @param tableName 表名
     * @return 敏感列列表
     */
    List<SensitiveColumn> detectSensitiveColumnsWithPresidioAndConnection(Connection connection, String tableName);
} 