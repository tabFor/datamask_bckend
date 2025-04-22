package com.example.repository;

import com.example.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    
    List<AuditLog> findByTimestampBetweenAndOperationAndStatus(
        LocalDateTime startTime, 
        LocalDateTime endTime, 
        String operation,
        String status
    );

    @Query("SELECT DISTINCT a.operation FROM AuditLog a")
    List<String> findDistinctOperations();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.operation = :operation AND a.timestamp BETWEEN :startTime AND :endTime")
    long countByOperationAndTimeRange(
        @Param("operation") String operation,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
} 