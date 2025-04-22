package com.example.repository;

import com.example.model.FinancialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 金融记录仓库接口
 */
@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {
    
    /**
     * 根据客户ID查询金融记录
     * @param customerId 客户ID
     * @return 该客户的金融记录列表
     */
    List<FinancialRecord> findByCustomerId(Long customerId);
    
    /**
     * 根据交易类型查询金融记录
     * @param transactionType 交易类型
     * @return 指定交易类型的金融记录列表
     */
    List<FinancialRecord> findByTransactionType(String transactionType);
    
    /**
     * 根据余额范围查询金融记录
     * @param minBalance 最小余额
     * @param maxBalance 最大余额
     * @return 余额在指定范围内的金融记录列表
     */
    List<FinancialRecord> findByBalanceBetween(BigDecimal minBalance, BigDecimal maxBalance);
} 