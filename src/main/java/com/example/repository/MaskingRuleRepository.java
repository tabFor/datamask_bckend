package com.example.repository;

import com.example.model.MaskingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaskingRuleRepository extends JpaRepository<MaskingRuleEntity, Long> {
    
    List<MaskingRuleEntity> findByTableName(String tableName);
    
    List<MaskingRuleEntity> findByTableNameAndActiveTrue(String tableName);
    
    List<MaskingRuleEntity> findByDatabaseAndTableName(String database, String tableName);
    
    List<MaskingRuleEntity> findByDatabaseAndTableNameAndActiveTrue(String database, String tableName);
    
    List<MaskingRuleEntity> findByActiveIsTrue();
} 