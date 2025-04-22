package com.example.repository;

import com.example.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 医疗记录仓库接口
 */
@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    
    /**
     * 根据患者ID查询医疗记录
     * @param patientId 患者ID
     * @return 该患者的医疗记录列表
     */
    List<MedicalRecord> findByPatientId(Long patientId);
    
    /**
     * 根据血型查询医疗记录
     * @param bloodType 血型
     * @return 指定血型的医疗记录列表
     */
    List<MedicalRecord> findByBloodType(String bloodType);
    
    /**
     * 根据就诊日期范围查询医疗记录
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 就诊日期在指定范围内的医疗记录列表
     */
    List<MedicalRecord> findByVisitDateBetween(Date startDate, Date endDate);
    
    /**
     * 根据诊断结果模糊查询医疗记录
     * @param diagnosis 诊断结果（支持模糊匹配）
     * @return 匹配的医疗记录列表
     */
    List<MedicalRecord> findByDiagnosisContaining(String diagnosis);
} 