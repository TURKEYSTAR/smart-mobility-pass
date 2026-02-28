package com.smartmobility.tripservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
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

    @Value("${rabbitmq.routing-key.trip-completed}")
    private String tripCompletedRoutingKey;

    @Value("${rabbitmq.routing-key.pricing-fallback}")
    private String pricingFallbackRoutingKey;

    @Bean
    public TopicExchange smartMobilityExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue tripCompletedQueue() {
        return QueueBuilder.durable(tripCompletedQueue).build();
    }

    @Bean
    public Queue pricingFallbackQueue() {
        return QueueBuilder.durable(pricingFallbackQueue).build();
    }

    @Bean
    public Binding tripCompletedBinding() {
        return BindingBuilder.bind(tripCompletedQueue())
                .to(smartMobilityExchange())
                .with(tripCompletedRoutingKey);
    }

    @Bean
    public Binding pricingFallbackBinding() {
        return BindingBuilder.bind(pricingFallbackQueue())
                .to(smartMobilityExchange())
                .with(pricingFallbackRoutingKey);
    }

//    @Bean
//    public MessageConverter jsonMessageConverter() {
//        return new SimpleMessageConverter();
//    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
