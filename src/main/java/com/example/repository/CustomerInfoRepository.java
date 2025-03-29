package com.example.repository;

import com.example.model.CustomerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 客户信息仓库接口
 */
@Repository
public interface CustomerInfoRepository extends JpaRepository<CustomerInfo, Long> {
    
    /**
     * 根据姓名模糊查询客户信息
     * @param name 姓名（支持模糊匹配）
     * @return 匹配的客户信息列表
     */
    List<CustomerInfo> findByNameContaining(String name);
    
    /**
     * 根据年龄范围查询客户信息
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 年龄在指定范围内的客户信息列表
     */
    List<CustomerInfo> findByAgeBetween(Integer minAge, Integer maxAge);
    
    /**
     * 根据性别查询客户信息
     * @param gender 性别
     * @return 指定性别的客户信息列表
     */
    List<CustomerInfo> findByGender(String gender);
}