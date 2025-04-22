package com.example.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * 脱敏数据服务
 * 提供数据查看、预览和下载功能
 */
@Service
public interface MaskedDataService {
    
    /**
     * 获取所有脱敏数据文件列表
     */
    List<Map<String, Object>> listMaskedFiles();
    
    /**
     * 预览脱敏数据（分页）
     */
    Map<String, Object> previewMaskedData(String filePath, int page, int size);
    
    /**
     * 获取脱敏数据文件作为下载资源
     */
    Resource getMaskedFileAsResource(String filePath);
    
    /**
     * 查询脱敏数据库表（带分页和条件）
     */
    Map<String, Object> queryMaskedTable(String tableName, int page, int size, String conditionsJson);
    
    /**
     * 获取可查询的脱敏数据表列表
     */
    List<String> listMaskedTables();
    
    /**
     * 更新脱敏数据
     * @param tableName 表名
     * @param data 更新的数据
     */
    void updateMaskedData(String tableName, Map<String, Object> data);
    
    /**
     * 删除脱敏数据
     * @param tableName 表名
     */
    void deleteMaskedData(String tableName);
    
    /**
     * 清除表相关的所有缓存
     * @param tableName 表名
     */
    void clearTableCache(String tableName);
    
    /**
     * 清除所有脱敏数据相关的缓存
     */
    void clearAllCaches();
} 