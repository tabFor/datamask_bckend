package com.example.service;

import com.example.config.RabbitMQConfig;
import com.example.service.impl.TaskServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.amqp.core.Message;

@Service
public class RabbitMQConsumerService {

    @Autowired
    private TaskServiceImpl taskService;

    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.TASK_EXECUTION_QUEUE)
    public void receiveMessage(Message message) {
        try {
            System.out.println("收到任务执行消息: " + message);
            
            // 从消息体中获取任务ID
            String messageBody = new String(message.getBody());
            TaskExecutionMessage executionMessage;
            try {
                executionMessage = objectMapper.readValue(messageBody, TaskExecutionMessage.class);
            } catch (JsonProcessingException e) {
                System.err.println("解析消息失败: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            Long taskId = executionMessage.getTaskId();
            if (taskId == null) {
                System.err.println("消息中缺少任务ID");
                return;
            }
            
            // 执行任务
            taskService.executeTask(taskId);
            
            System.out.println("任务执行完成, 任务ID: " + taskId);
        } catch (Exception e) {
            System.err.println("处理任务执行消息失败: " + e.getMessage());
            e.printStackTrace();
            // 如果是任务状态不允许执行的错误，不需要重试
            if (e.getMessage() != null && e.getMessage().contains("任务状态不允许执行")) {
                System.out.println("任务状态不允许执行，消息将被丢弃");
                return;
            }
            // 其他错误可能需要重试
            throw e;
        }
    }

    private static class TaskExecutionMessage {
        private Long taskId;
        private Object taskDTO;

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public Object getTaskDTO() {
            return taskDTO;
        }

        public void setTaskDTO(Object taskDTO) {
            this.taskDTO = taskDTO;
        }
    }
} 