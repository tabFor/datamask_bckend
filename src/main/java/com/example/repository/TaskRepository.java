package com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByTaskNameContaining(String keyword, Pageable pageable);
} 