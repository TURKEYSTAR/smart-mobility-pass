package com.smartmobility.notificationservice.config;

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

    @Value("${rabbitmq.queue.trip-completed}")
    private String tripCompletedQueue;

    @Value("${rabbitmq.queue.pricing-fallback}")
    private String pricingFallbackQueue;

    @Value("${rabbitmq.queue.insufficient-balance}")
    private String insufficientBalanceQueue;

    @Value("${rabbitmq.queue.pass-suspended}")
    private String passSuspendedQueue;

    @Value("${rabbitmq.routing-key.trip-completed}")
    private String tripCompletedKey;

    @Value("${rabbitmq.routing-key.pricing-fallback}")
    private String pricingFallbackKey;

    @Value("${rabbitmq.routing-key.insufficient-balance}")
    private String insufficientBalanceKey;

    @Value("${rabbitmq.routing-key.pass-suspended}")
    private String passSuspendedKey;

    @Bean
    public TopicExchange smartMobilityExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean public Queue tripCompletedQueue()       { return QueueBuilder.durable(tripCompletedQueue).build(); }
    @Bean public Queue pricingFallbackQueue()     { return QueueBuilder.durable(pricingFallbackQueue).build(); }
    @Bean public Queue insufficientBalanceQueue() { return QueueBuilder.durable(insufficientBalanceQueue).build(); }
    @Bean public Queue passSuspendedQueue()       { return QueueBuilder.durable(passSuspendedQueue).build(); }

    @Bean public Binding tripCompletedBinding() {
        return BindingBuilder.bind(tripCompletedQueue()).to(smartMobilityExchange()).with(tripCompletedKey);
    }
    @Bean public Binding pricingFallbackBinding() {
        return BindingBuilder.bind(pricingFallbackQueue()).to(smartMobilityExchange()).with(pricingFallbackKey);
    }
    @Bean public Binding insufficientBalanceBinding() {
        return BindingBuilder.bind(insufficientBalanceQueue()).to(smartMobilityExchange()).with(insufficientBalanceKey);
    }
    @Bean public Binding passSuspendedBinding() {
        return BindingBuilder.bind(passSuspendedQueue()).to(smartMobilityExchange()).with(passSuspendedKey);
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