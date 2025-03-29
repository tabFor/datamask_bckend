package com.example.repository;

import com.example.model.EmployeeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 员工数据仓库接口
 */
@Repository
public interface EmployeeDataRepository extends JpaRepository<EmployeeData, Long> {
    
    /**
     * 根据部门查询员工数据
     * @param department 部门名称
     * @return 指定部门的员工数据列表
     */
    List<EmployeeData> findByDepartment(String department);
    
    /**
     * 根据职位查询员工数据
     * @param position 职位名称
     * @return 指定职位的员工数据列表
     */
    List<EmployeeData> findByPosition(String position);
    
    /**
     * 根据薪资范围查询员工数据
     * @param minSalary 最低薪资
     * @param maxSalary 最高薪资
     * @return 薪资在指定范围内的员工数据列表
     */
    List<EmployeeData> findBySalaryBetween(BigDecimal minSalary, BigDecimal maxSalary);
    
    /**
     * 根据入职日期范围查询员工数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 入职日期在指定范围内的员工数据列表
     */
    List<EmployeeData> findByHireDateBetween(Date startDate, Date endDate);
    
    /**
     * 根据绩效评分范围查询员工数据
     * @param minRating 最低评分
     * @param maxRating 最高评分
     * @return 绩效评分在指定范围内的员工数据列表
     */
    List<EmployeeData> findByPerformanceRatingBetween(BigDecimal minRating, BigDecimal maxRating);
} 