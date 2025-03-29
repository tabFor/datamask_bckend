package com.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${spring.rabbitmq.host}")
    private String host;
    
    @Value("${spring.rabbitmq.port}")
    private int port;
    
    @Value("${spring.rabbitmq.username}")
    private String username;
    
    @Value("${spring.rabbitmq.password}")
    private String password;
    
    // 任务执行相关的常量
    public static final String TASK_EXECUTION_QUEUE = "task_execution_queue";
    public static final String TASK_EXECUTION_EXCHANGE = "task_execution_exchange";
    public static final String TASK_EXECUTION_ROUTING_KEY = "task_execution_routing_key";

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setChannelCacheSize(25);
        connectionFactory.setRequestedHeartBeat(30);
        return connectionFactory;
    }

    @Bean
    public Queue taskExecutionQueue() {
        return new Queue(TASK_EXECUTION_QUEUE);
    }

    @Bean
    public DirectExchange taskExecutionExchange() {
        return new DirectExchange(TASK_EXECUTION_EXCHANGE);
    }

    @Bean
    public Binding taskExecutionBinding(Queue taskExecutionQueue, DirectExchange taskExecutionExchange) {
        return BindingBuilder
                .bind(taskExecutionQueue)
                .to(taskExecutionExchange)
                .with(TASK_EXECUTION_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
} 