package com.example.service;

import com.example.config.RabbitMQConfig;
import com.example.dto.TaskDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQProducerService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendTaskExecutionMessage(Long taskId, TaskDTO taskDTO) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TASK_EXECUTION_EXCHANGE,
            RabbitMQConfig.TASK_EXECUTION_ROUTING_KEY,
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