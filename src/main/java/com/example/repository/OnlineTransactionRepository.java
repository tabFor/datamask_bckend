package com.example.repository;

import com.example.model.OnlineTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 在线交易仓库接口
 */
@Repository
public interface OnlineTransactionRepository extends JpaRepository<OnlineTransaction, Long> {
    
    /**
     * 根据用户ID查询在线交易
     * @param userId 用户ID
     * @return 该用户的在线交易列表
     */
    List<OnlineTransaction> findByUserId(Long userId);
    
    /**
     * 根据订单ID查询在线交易
     * @param orderId 订单ID
     * @return 匹配的在线交易列表
     */
    List<OnlineTransaction> findByOrderId(String orderId);
    
    /**
     * 根据产品名称模糊查询在线交易
     * @param productName 产品名称（支持模糊匹配）
     * @return 匹配的在线交易列表
     */
    List<OnlineTransaction> findByProductNameContaining(String productName);
    
    /**
     * 根据价格范围查询在线交易
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 价格在指定范围内的在线交易列表
     */
    List<OnlineTransaction> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    /**
     * 根据支付方式查询在线交易
     * @param paymentMethod 支付方式
     * @return 指定支付方式的在线交易列表
     */
    List<OnlineTransaction> findByPaymentMethod(String paymentMethod);
    
    /**
     * 根据交易日期范围查询在线交易
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 交易日期在指定范围内的在线交易列表
     */
    List<OnlineTransaction> findByTransactionDateBetween(Date startDate, Date endDate);
} 