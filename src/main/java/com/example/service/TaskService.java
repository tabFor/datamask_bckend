package com.example.service;

import com.example.dto.TaskDTO;
import com.example.model.Task;
import org.springframework.data.domain.Page;

public interface TaskService {
    Page<Task> findTasks(int page, int pageSize, String keyword);
    
    Task createTask(TaskDTO taskDTO);
    
    Task findTaskById(Long id);
    
    Task executeTask(Long id);
    
    void deleteTask(Long id);
    
    Task updateTask(Long id, TaskDTO taskDTO);

    Task getTaskById(Long id);

    Task updateTaskStatus(Long id, String status);
} 