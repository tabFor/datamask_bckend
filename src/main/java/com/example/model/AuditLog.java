package com.example.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String operation;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private String status;
} 