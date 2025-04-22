package com.example.controller;

import com.example.model.*;
import com.example.repository.*;
import com.example.service.MaskingRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测试数据控制器
 * 用于查询和展示测试数据
 */
@RestController
@RequestMapping("/api/test-data")
public class TestDataController {

    private final CustomerInfoRepository customerInfoRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final EmployeeDataRepository employeeDataRepository;
    private final OnlineTransactionRepository onlineTransactionRepository;
    private final MaskingRuleService maskingRuleService;

    @Autowired
    public TestDataController(
            CustomerInfoRepository customerInfoRepository,
            FinancialRecordRepository financialRecordRepository,
            MedicalRecordRepository medicalRecordRepository,
            EmployeeDataRepository employeeDataRepository,
            OnlineTransactionRepository onlineTransactionRepository,
            MaskingRuleService maskingRuleService) {
        this.customerInfoRepository = customerInfoRepository;
        this.financialRecordRepository = financialRecordRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.employeeDataRepository = employeeDataRepository;
        this.onlineTransactionRepository = onlineTransactionRepository;
        this.maskingRuleService = maskingRuleService;
    }

    /**
     * 获取所有测试数据的统计信息
     * @return 各表的数据条数
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDataStats() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("customerInfo", customerInfoRepository.count());
        stats.put("financialRecord", financialRecordRepository.count());
        stats.put("medicalRecord", medicalRecordRepository.count());
        stats.put("employeeData", employeeDataRepository.count());
        stats.put("onlineTransaction", onlineTransactionRepository.count());
        
        response.put("success", true);
        response.put("data", stats);
        return ResponseEntity.ok(response);
    }

    // ==================== 客户信息接口 ====================

    /**
     * 获取客户信息列表（分页）
     * @param page 页码（从0开始）
     * @param size 每页记录数
     * @param enableMasking 是否启用脱敏
     * @return 客户信息分页列表
     */
    @GetMapping("/customer_info")
    public ResponseEntity<Map<String, Object>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean enableMasking) {
        
        // 如果启用脱敏，刷新脱敏规则
        if (enableMasking) {
            System.out.println("启用脱敏功能，刷新脱敏规则");
            // 使用正确的表名，与数据库一致
            maskingRuleService.refreshRules("customer_info");
        } else {
            System.out.println("未启用脱敏功能");
            // 清空脱敏规则
            maskingRuleService.clearMaskingRules();
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<CustomerInfo> customerPage = customerInfoRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("enableMasking", enableMasking);
        response.put("data", new HashMap<String, Object>() {{
            put("content", customerPage.getContent());
            put("totalElements", customerPage.getTotalElements());
            put("totalPages", customerPage.getTotalPages());
            put("currentPage", customerPage.getNumber());
        }});
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取客户信息
     * @param id 客户ID
     * @return 客户信息详情
     */
    @GetMapping("/customer_info/{id}")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Long id) {
        Optional<CustomerInfo> customer = customerInfoRepository.findById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (customer.isPresent()) {
            response.put("success", true);
            response.put("data", customer.get());
        } else {
            response.put("success", false);
            response.put("message", "未找到ID为" + id + "的客户信息");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据姓名搜索客户信息
     * @param name 客户姓名（支持模糊搜索）
     * @return 匹配的客户信息列表
     */
    @GetMapping("/customer_info/search/name")
    public ResponseEntity<Map<String, Object>> searchCustomersByName(@RequestParam String name) {
        List<CustomerInfo> customers = customerInfoRepository.findByNameContaining(name);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", customers);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据年龄范围搜索客户信息
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 年龄在指定范围内的客户信息列表
     */
    @GetMapping("/customer_info/search/age")
    public ResponseEntity<Map<String, Object>> searchCustomersByAgeBetween(
            @RequestParam Integer minAge,
            @RequestParam Integer maxAge) {
        List<CustomerInfo> customers = customerInfoRepository.findByAgeBetween(minAge, maxAge);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", customers);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据性别搜索客户信息
     * @param gender 性别
     * @return 指定性别的客户信息列表
     */
    @GetMapping("/customer_info/search/gender")
    public ResponseEntity<Map<String, Object>> searchCustomersByGender(@RequestParam String gender) {
        List<CustomerInfo> customers = customerInfoRepository.findByGender(gender);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", customers);
        
        return ResponseEntity.ok(response);
    }

    // ==================== 金融记录接口 ====================

    /**
     * 获取金融记录列表（分页）
     * @param page 页码（从0开始）
     * @param size 每页记录数
     * @param enableMasking 是否启用脱敏
     * @return 金融记录分页列表
     */
    @GetMapping("/financial_records")
    public ResponseEntity<Map<String, Object>> getFinancialRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean enableMasking) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<FinancialRecord> recordPage = financialRecordRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", new HashMap<String, Object>() {{
            put("content", recordPage.getContent());
            put("totalElements", recordPage.getTotalElements());
            put("totalPages", recordPage.getTotalPages());
            put("currentPage", recordPage.getNumber());
        }});
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取金融记录
     * @param id 记录ID
     * @return 金融记录详情
     */
    @GetMapping("/financial_records/{id}")
    public ResponseEntity<Map<String, Object>> getFinancialRecordById(@PathVariable Long id) {
        Optional<FinancialRecord> record = financialRecordRepository.findById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (record.isPresent()) {
            response.put("success", true);
            response.put("data", record.get());
        } else {
            response.put("success", false);
            response.put("message", "未找到ID为" + id + "的金融记录");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据客户ID获取金融记录
     * @param customerId 客户ID
     * @return 该客户的金融记录列表
     */
    @GetMapping("/financial_records/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getFinancialRecordsByCustomerId(@PathVariable Long customerId) {
        List<FinancialRecord> records = financialRecordRepository.findByCustomerId(customerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据交易类型查询金融记录
     * @param transactionType 交易类型
     * @return 指定交易类型的金融记录列表
     */
    @GetMapping("/financial_records/search/transaction-type")
    public ResponseEntity<Map<String, Object>> getFinancialRecordsByTransactionType(
            @RequestParam String transactionType) {
        List<FinancialRecord> records = financialRecordRepository.findByTransactionType(transactionType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据余额范围查询金融记录
     * @param minBalance 最小余额
     * @param maxBalance 最大余额
     * @return 余额在指定范围内的金融记录列表
     */
    @GetMapping("/financial_records/search/balance")
    public ResponseEntity<Map<String, Object>> getFinancialRecordsByBalanceBetween(
            @RequestParam BigDecimal minBalance,
            @RequestParam BigDecimal maxBalance) {
        List<FinancialRecord> records = financialRecordRepository.findByBalanceBetween(minBalance, maxBalance);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        
        return ResponseEntity.ok(response);
    }

    // ==================== 医疗记录接口 ====================

    /**
     * 获取医疗记录列表（分页）
     * @param page 页码（从0开始）
     * @param size 每页记录数
     * @param enableMasking 是否启用脱敏
     * @return 医疗记录分页列表
     */
    @GetMapping("/medical_records")
    public ResponseEntity<Map<String, Object>> getMedicalRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean enableMasking) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<MedicalRecord> recordPage = medicalRecordRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", new HashMap<String, Object>() {{
            put("content", recordPage.getContent());
            put("totalElements", recordPage.getTotalElements());
            put("totalPages", recordPage.getTotalPages());
            put("currentPage", recordPage.getNumber());
        }});
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取医疗记录
     * @param id 记录ID
     * @return 医疗记录详情
     */
    @GetMapping("/medical_records/{id}")
    public ResponseEntity<Map<String, Object>> getMedicalRecordById(@PathVariable Long id) {
        Optional<MedicalRecord> record = medicalRecordRepository.findById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (record.isPresent()) {
            response.put("success", true);
            response.put("data", record.get());
        } else {
            response.put("success", false);
            response.put("message", "未找到ID为" + id + "的医疗记录");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据患者ID获取医疗记录
     * @param patientId 患者ID
     * @return 该患者的医疗记录列表
     */
    @GetMapping("/medical_records/patient/{patientId}")
    public ResponseEntity<Map<String, Object>> getMedicalRecordsByPatientId(@PathVariable Long patientId) {
        List<MedicalRecord> records = medicalRecordRepository.findByPatientId(patientId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据血型查询医疗记录
     * @param bloodType 血型
     * @return 指定血型的医疗记录列表
     */
    @GetMapping("/medical_records/search/blood-type")
    public ResponseEntity<Map<String, Object>> getMedicalRecordsByBloodType(@RequestParam String bloodType) {
        List<MedicalRecord> records = medicalRecordRepository.findByBloodType(bloodType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据就诊日期范围查询医疗记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 就诊日期在指定范围内的医疗记录列表
     */
    @GetMapping("/medical_records/search/visit-date")
    public ResponseEntity<Map<String, Object>> getMedicalRecordsByVisitDateBetween(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<MedicalRecord> records = medicalRecordRepository.findByVisitDateBetween(startDate, endDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据诊断内容查询医疗记录
     * @param diagnosis 诊断内容（支持模糊搜索）
     * @return 诊断内容包含指定关键词的医疗记录列表
     */
    @GetMapping("/medical_records/search/diagnosis")
    public ResponseEntity<Map<String, Object>> getMedicalRecordsByDiagnosisContaining(
            @RequestParam String diagnosis) {
        List<MedicalRecord> records = medicalRecordRepository.findByDiagnosisContaining(diagnosis);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", records);
        
        return ResponseEntity.ok(response);
    }

    // ==================== 员工数据接口 ====================

    /**
     * 获取员工数据列表（分页）
     * @param page 页码（从0开始）
     * @param size 每页记录数
     * @param enableMasking 是否启用脱敏
     * @return 员工数据分页列表
     */
    @GetMapping("/employee_data")
    public ResponseEntity<Map<String, Object>> getEmployeeData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean enableMasking) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<EmployeeData> employeePage = employeeDataRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", new HashMap<String, Object>() {{
            put("content", employeePage.getContent());
            put("totalElements", employeePage.getTotalElements());
            put("totalPages", employeePage.getTotalPages());
            put("currentPage", employeePage.getNumber());
        }});
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取员工数据
     * @param id 员工数据ID
     * @return 员工数据详情
     */
    @GetMapping("/employee_data/{id}")
    public ResponseEntity<Map<String, Object>> getEmployeeDataById(@PathVariable Long id) {
        Optional<EmployeeData> employee = employeeDataRepository.findById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (employee.isPresent()) {
            response.put("success", true);
            response.put("data", employee.get());
        } else {
            response.put("success", false);
            response.put("message", "未找到ID为" + id + "的员工数据");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据部门查询员工数据
     * @param department 部门名称
     * @return 该部门的员工数据列表
     */
    @GetMapping("/employee_data/search/department")
    public ResponseEntity<Map<String, Object>> getEmployeeDataByDepartment(@RequestParam String department) {
        List<EmployeeData> employees = employeeDataRepository.findByDepartment(department);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", employees);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据职位查询员工数据
     * @param position 职位名称
     * @return 该职位的员工数据列表
     */
    @GetMapping("/employee_data/search/position")
    public ResponseEntity<Map<String, Object>> getEmployeeDataByPosition(@RequestParam String position) {
        List<EmployeeData> employees = employeeDataRepository.findByPosition(position);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", employees);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据薪资范围查询员工数据
     * @param minSalary 最低薪资
     * @param maxSalary 最高薪资
     * @return 薪资在指定范围内的员工数据列表
     */
    @GetMapping("/employee_data/search/salary")
    public ResponseEntity<Map<String, Object>> getEmployeeDataBySalaryBetween(
            @RequestParam BigDecimal minSalary,
            @RequestParam BigDecimal maxSalary) {
        List<EmployeeData> employees = employeeDataRepository.findBySalaryBetween(minSalary, maxSalary);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", employees);
        
        return ResponseEntity.ok(response);
    }

    // ==================== 在线交易接口 ====================

    /**
     * 获取在线交易列表（分页）
     * @param page 页码（从0开始）
     * @param size 每页记录数
     * @param enableMasking 是否启用脱敏
     * @return 在线交易分页列表
     */
    @GetMapping("/online_transactions")
    public ResponseEntity<Map<String, Object>> getOnlineTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean enableMasking) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<OnlineTransaction> transactionPage = onlineTransactionRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", new HashMap<String, Object>() {{
            put("content", transactionPage.getContent());
            put("totalElements", transactionPage.getTotalElements());
            put("totalPages", transactionPage.getTotalPages());
            put("currentPage", transactionPage.getNumber());
        }});
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取在线交易
     * @param id 交易ID
     * @return 在线交易详情
     */
    @GetMapping("/online_transactions/{id}")
    public ResponseEntity<Map<String, Object>> getOnlineTransactionById(@PathVariable Long id) {
        Optional<OnlineTransaction> transaction = onlineTransactionRepository.findById(id);
        
        Map<String, Object> response = new HashMap<>();
        if (transaction.isPresent()) {
            response.put("success", true);
            response.put("data", transaction.get());
        } else {
            response.put("success", false);
            response.put("message", "未找到ID为" + id + "的在线交易");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据用户ID获取在线交易
     * @param userId 用户ID
     * @return 该用户的在线交易列表
     */
    @GetMapping("/online_transactions/user/{userId}")
    public ResponseEntity<Map<String, Object>> getOnlineTransactionsByUserId(@PathVariable Long userId) {
        List<OnlineTransaction> transactions = onlineTransactionRepository.findByUserId(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据订单ID查询在线交易
     * @param orderId 订单ID
     * @return 指定订单ID的在线交易
     */
    @GetMapping("/online_transactions/search/order-id")
    public ResponseEntity<Map<String, Object>> getOnlineTransactionsByOrderId(@RequestParam String orderId) {
        List<OnlineTransaction> transactions = onlineTransactionRepository.findByOrderId(orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据产品名称查询在线交易
     * @param productName 产品名称（支持模糊搜索）
     * @return 产品名称包含指定关键词的在线交易列表
     */
    @GetMapping("/online_transactions/search/product")
    public ResponseEntity<Map<String, Object>> getOnlineTransactionsByProductNameContaining(
            @RequestParam String productName) {
        List<OnlineTransaction> transactions = onlineTransactionRepository.findByProductNameContaining(productName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根据支付方式查询在线交易
     * @param paymentMethod 支付方式
     * @return 指定支付方式的在线交易列表
     */
    @GetMapping("/online_transactions/search/payment-method")
    public ResponseEntity<Map<String, Object>> getOnlineTransactionsByPaymentMethod(
            @RequestParam String paymentMethod) {
        List<OnlineTransaction> transactions = onlineTransactionRepository.findByPaymentMethod(paymentMethod);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);
        
        return ResponseEntity.ok(response);
    }
} 