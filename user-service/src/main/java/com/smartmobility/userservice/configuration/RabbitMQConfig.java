package com.smartmobility.userservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.pass-suspended}")
    private String passSuspendedQueue;

    @Value("${rabbitmq.routing-key.pass-suspended}")
    private String passSuspendedRoutingKey;

    @Bean
    public TopicExchange smartMobilityExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue passSuspendedQueue() {
        return QueueBuilder.durable(passSuspendedQueue).build();
    }

    @Bean
    public Binding passSuspendedBinding() {
        return BindingBuilder.bind(passSuspendedQueue())
                .to(smartMobilityExchange())
                .with(passSuspendedRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate t = new RabbitTemplate(connectionFactory);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
