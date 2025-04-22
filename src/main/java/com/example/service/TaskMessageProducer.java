package com.example.service;

import com.example.dto.TaskDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendTaskExecutionMessage(Long taskId, TaskDTO taskDTO) {
        rabbitTemplate.convertAndSend(
            "task_execution_exchange",
            "task_execution_routing_key",
            new TaskExecutionMessage(taskId, taskDTO)
        );
    }

    private static class TaskExecutionMessage {
        private final Long taskId;
        private final TaskDTO taskDTO;

        public TaskExecutionMessage(Long taskId, TaskDTO taskDTO) {
            this.taskId = taskId;
            this.taskDTO = taskDTO;
        }

        // Getters
        public Long getTaskId() { return taskId; }
        public TaskDTO getTaskDTO() { return taskDTO; }
    }
} 