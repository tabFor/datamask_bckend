package com.example.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工数据实体类
 * 映射employee_data表，包含工作和薪资信息
 */
@Entity
@Table(name = "employee_data")
public class EmployeeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", length = 20)
    private String employeeId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "position", length = 50)
    private String position;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "bonus", precision = 12, scale = 2)
    private BigDecimal bonus;

    @Column(name = "bank_account", length = 20)
    private String bankAccount;

    @Column(name = "social_security", length = 20)
    private String socialSecurity;

    @Column(name = "hire_date")
    @Temporal(TemporalType.DATE)
    private Date hireDate;

    @Column(name = "performance_rating", precision = 3, scale = 1)
    private BigDecimal performanceRating;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getSocialSecurity() {
        return socialSecurity;
    }

    public void setSocialSecurity(String socialSecurity) {
        this.socialSecurity = socialSecurity;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    public BigDecimal getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(BigDecimal performanceRating) {
        this.performanceRating = performanceRating;
    }

    @Override
    public String toString() {
        return "EmployeeData{" +
                "id=" + id +
                ", employeeId='" + employeeId + '\'' +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", position='" + position + '\'' +
                ", salary=" + salary +
                ", bonus=" + bonus +
                ", bankAccount='" + bankAccount + '\'' +
                ", socialSecurity='" + socialSecurity + '\'' +
                ", hireDate=" + hireDate +
                ", performanceRating=" + performanceRating +
                '}';
    }
} 