package com.example.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 医疗记录实体类
 * 映射medical_records表，包含健康和医疗信息
 */
@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private CustomerInfo patient;

    @Column(name = "blood_type", length = 5)
    private String bloodType;

    @Column(name = "height", precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(name = "diagnosis", length = 200)
    private String diagnosis;

    @Column(name = "medication", length = 200)
    private String medication;

    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;

    @Column(name = "visit_date")
    @Temporal(TemporalType.DATE)
    private Date visitDate;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerInfo getPatient() {
        return patient;
    }

    public void setPatient(CustomerInfo patient) {
        this.patient = patient;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

    public Date getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
    }

    @Override
    public String toString() {
        return "MedicalRecord{" +
                "id=" + id +
                ", patient=" + patient.getId() +
                ", bloodType='" + bloodType + '\'' +
                ", height=" + height +
                ", weight=" + weight +
                ", medicalHistory='" + medicalHistory + '\'' +
                ", diagnosis='" + diagnosis + '\'' +
                ", medication='" + medication + '\'' +
                ", doctorNotes='" + doctorNotes + '\'' +
                ", visitDate=" + visitDate +
                '}';
    }
} 